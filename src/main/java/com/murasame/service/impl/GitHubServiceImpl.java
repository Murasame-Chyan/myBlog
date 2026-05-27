package com.murasame.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.murasame.client.GitHubApiClient;
import com.murasame.domain.dto.GitHubAchievementDTO;
import com.murasame.domain.dto.GitHubRepoDTO;
import com.murasame.domain.vo.GitHubProfileVO;
import com.murasame.service.GitHubService;
import jakarta.annotation.Resource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class GitHubServiceImpl implements GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubServiceImpl.class);
    private static final String REDIS_KEY_PREFIX = "github:profile:";
    private static final String REDIS_NEG_KEY_PREFIX = "github:profile:neg:";
    private static final Duration REDIS_TTL = Duration.ofHours(2);
    // 失败负缓存：GitHub 抖动时短期挡住雪崩重试，避免每个请求都把后端打慢
    private static final Duration REDIS_NEG_TTL = Duration.ofMinutes(5);
    // 单次子任务超时（三路并行，总耗时 ≈ max 而非 sum，仍远小于网关 60s）
    private static final Duration TASK_TIMEOUT = Duration.ofSeconds(15);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_RETRIES = 2;

    // 三个 GitHub 调用并行用，固定大小池防止突发流量打爆
    private static final ExecutorService GITHUB_EXECUTOR =
            Executors.newFixedThreadPool(6, r -> {
                Thread t = new Thread(r, "github-fetch");
                t.setDaemon(true);
                return t;
            });

    private static final String BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    // GitHub 绿阶 → level 映射
    private static final Map<String, Integer> COLOR_TO_LEVEL = Map.of(
            "#ebedf0", 0,
            "#9be9a8", 1,
            "#40c463", 2,
            "#30a14e", 3,
            "#216e39", 4
    );

    @Resource
    private GitHubApiClient apiClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${github.disable-ssl-verification:false}")
    private boolean disableSslVerification;

    @Override
    public GitHubProfileVO getGitHubProfile(String username, String userToken) {
        // 1. 查 Redis 正向缓存（不同用户的 token 可能不同，但热力图数据是公开的，按 username 缓存即可）
        String redisKey = REDIS_KEY_PREFIX + username;
        String cached = stringRedisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, GitHubProfileVO.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize cached GitHub profile, re-fetching: {}", e.getMessage());
            }
        }

        // 2. 查负缓存：刚刚失败过的就直接返回空 VO，避免雪崩重试把后端打慢触发 502
        String negKey = REDIS_NEG_KEY_PREFIX + username;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(negKey))) {
            log.info("GitHub profile negative cache hit, returning empty VO: username={}", username);
            return emptyVo();
        }

        // 3. 三路并行：GraphQL 热力图 / Profile 页面（成就 + 热力图降级共用） / REST 仓库列表
        //    总耗时 ≈ max(三者) 而不是 sum(三者)，且整体超时 OVERALL_TIMEOUT 之内必返回。
        CompletableFuture<HeatmapResult> heatmapFuture = CompletableFuture.supplyAsync(
                () -> fetchHeatmapViaGraphQL(username, userToken), GITHUB_EXECUTOR);

        CompletableFuture<Document> profilePageFuture = CompletableFuture.supplyAsync(
                () -> fetchProfileQuietly(username), GITHUB_EXECUTOR);

        CompletableFuture<List<GitHubRepoDTO>> reposFuture = CompletableFuture.supplyAsync(
                () -> fetchReposQuietly(username, userToken), GITHUB_EXECUTOR);

        GitHubProfileVO vo = new GitHubProfileVO();
        boolean heatmapOk;

        try {
            HeatmapResult heatmap = heatmapFuture.get(TASK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            // GraphQL 失败就用 profile 页面降级解析（profile 页面已经在并行抓了）
            if (!heatmap.ok()) {
                Document doc = waitForProfile(profilePageFuture);
                heatmap = doc != null ? parseHeatmapFromDoc(doc) : HeatmapResult.empty();
            }
            vo.setHeatmap(heatmap.heatmap());
            vo.setTotalContributions(heatmap.total());
            heatmapOk = heatmap.ok();
        } catch (TimeoutException | InterruptedException | java.util.concurrent.ExecutionException e) {
            log.warn("GitHub heatmap fetch timeout/interrupted (username={}): {}", username, e.getMessage());
            vo.setHeatmap(new ArrayList<>());
            vo.setTotalContributions(0);
            heatmapOk = false;
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        }

        // 成就：复用并行抓到的 profile 页面（避免重复请求 GitHub）
        try {
            Document doc = waitForProfile(profilePageFuture);
            vo.setAchievements(doc != null ? parseAchievements(doc) : new ArrayList<>());
        } catch (Exception e) {
            log.warn("获取 GitHub 成就失败 (username={}): {}", username, e.getMessage());
            vo.setAchievements(new ArrayList<>());
        }

        // 仓库
        try {
            vo.setRepos(reposFuture.get(TASK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            log.warn("获取 GitHub 仓库列表失败 (username={}): {}", username, e.getMessage());
            vo.setRepos(new ArrayList<>());
        }

        // 4. 写入缓存
        if (heatmapOk && vo.getHeatmap() != null && !vo.getHeatmap().isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(vo);
                stringRedisTemplate.opsForValue().set(redisKey, json, REDIS_TTL);
                log.info("GitHub profile cached in Redis: username={}, contributions={}", username, vo.getTotalContributions());
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize GitHub profile for cache: {}", e.getMessage());
            }
        } else {
            // 失败时写入负缓存，5 分钟内不再发起昂贵的真实请求
            stringRedisTemplate.opsForValue().set(negKey, "1", REDIS_NEG_TTL);
            log.warn("GitHub profile NOT cached, negative-cache set 5min: username={}, heatmapOk={}", username, heatmapOk);
        }

        return vo;
    }

    private GitHubProfileVO emptyVo() {
        GitHubProfileVO vo = new GitHubProfileVO();
        vo.setHeatmap(new ArrayList<>());
        vo.setTotalContributions(0);
        vo.setAchievements(new ArrayList<>());
        vo.setRepos(new ArrayList<>());
        return vo;
    }

    // 等待并行启动的 profile 抓取，加上单次任务超时保护
    private Document waitForProfile(CompletableFuture<Document> future) {
        try {
            return future.get(TASK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("等待 profile 页面抓取失败/超时: {}", e.getMessage());
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return null;
        }
    }

    // GraphQL 热力图：失败时返回 ok=false，由调用方决定是否降级
    private HeatmapResult fetchHeatmapViaGraphQL(String username, String userToken) {
        try {
            JsonNode cal = apiClient.fetchContributionCalendar(username, userToken);
            JsonNode calNode = cal.path("data").path("user").path("contributionsCollection").path("contributionCalendar");
            int total = calNode.path("totalContributions").asInt();
            JsonNode weeksNode = calNode.path("weeks");

            List<List<Map<String, Object>>> heatmap = new ArrayList<>();
            if (weeksNode.isArray()) {
                for (JsonNode weekNode : weeksNode) {
                    List<Map<String, Object>> week = new ArrayList<>();
                    JsonNode daysNode = weekNode.path("contributionDays");
                    if (daysNode.isArray()) {
                        for (JsonNode dayNode : daysNode) {
                            Map<String, Object> day = new HashMap<>();
                            day.put("date", dayNode.path("date").asText());
                            day.put("count", dayNode.path("contributionCount").asInt());
                            day.put("level", mapColorToLevel(dayNode.path("color").asText()));
                            week.add(day);
                        }
                    }
                    while (week.size() < 7) {
                        Map<String, Object> emptyDay = new HashMap<>();
                        emptyDay.put("date", "");
                        emptyDay.put("count", 0);
                        emptyDay.put("level", 0);
                        week.add(emptyDay);
                    }
                    heatmap.add(week);
                }
            }
            boolean ok = !heatmap.isEmpty();
            log.info("GitHub GraphQL 热力图获取{}: username={}, total={}, weeks={}",
                    ok ? "成功" : "失败(空数据)", username, total, heatmap.size());
            return new HeatmapResult(heatmap, total, ok);
        } catch (Exception e) {
            log.warn("GitHub GraphQL 失败，将降级到页面解析 (username={}): {}", username, e.getMessage());
            return HeatmapResult.empty();
        }
    }

    // 抓 profile 页面但不抛异常，便于在 CompletableFuture 里使用
    private Document fetchProfileQuietly(String username) {
        try {
            return fetchProfileWithRetry(username);
        } catch (Exception e) {
            log.warn("Profile 页面抓取失败 (username={}): {}", username, e.getMessage());
            return null;
        }
    }

    private List<GitHubRepoDTO> fetchReposQuietly(String username, String userToken) {
        try {
            return parseRepos(apiClient.listUserRepos(username, 1, 10, userToken));
        } catch (Exception e) {
            log.warn("仓库列表抓取失败 (username={}): {}", username, e.getMessage());
            return new ArrayList<>();
        }
    }

    private HeatmapResult parseHeatmapFromDoc(Document doc) {
        Map<String, Object> cr = parseContributions(doc);
        @SuppressWarnings("unchecked")
        List<List<Map<String, Object>>> heatmap = (List<List<Map<String, Object>>>) cr.get("heatmap");
        int total = (int) cr.getOrDefault("total", 0);
        return new HeatmapResult(heatmap == null ? new ArrayList<>() : heatmap, total,
                heatmap != null && !heatmap.isEmpty());
    }

    // 私有静态记录类：携带热力图 + 总贡献数 + 是否成功
    private record HeatmapResult(List<List<Map<String, Object>>> heatmap, int total, boolean ok) {
        static HeatmapResult empty() {
            return new HeatmapResult(new ArrayList<>(), 0, false);
        }
    }

    private int mapColorToLevel(String color) {
        return COLOR_TO_LEVEL.getOrDefault(color, 0);
    }

    @Override
    public List<GitHubRepoDTO> getRepos(String username, int page, int perPage, String userToken) {
        try {
            return parseRepos(apiClient.listUserRepos(username, page, perPage, userToken));
        } catch (Exception e) {
            log.warn("获取 GitHub 仓库列表失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // 带重试的 Profile 页面抓取（用于成就徽章和爬虫降级）
    private Document fetchProfileWithRetry(String username) throws IOException {
        Exception lastEx = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                var conn = Jsoup.connect("https://github.com/" + username)
                        .userAgent(BROWSER_UA)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                        .header("Accept-Language", "en-US,en;q=0.9,zh-CN;q=0.8")
                        .header("Accept-Encoding", "gzip, deflate, br")
                        .header("Cache-Control", "no-cache")
                        .header("Pragma", "no-cache")
                        .header("Sec-Ch-Ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                        .header("Sec-Ch-Ua-Mobile", "?0")
                        .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                        .header("Sec-Fetch-Dest", "document")
                        .header("Sec-Fetch-Mode", "navigate")
                        .header("Sec-Fetch-Site", "none")
                        .header("Sec-Fetch-User", "?1")
                        .header("Upgrade-Insecure-Requests", "1")
                        .timeout(6000);
                if (disableSslVerification) {
                    conn.sslContext(createTrustAllSslContext());
                }
                Document doc = conn.get();
                String title = doc.select("title").text();
                if (title.contains("Page not found")) {
                    throw new IOException("GitHub user not found: " + username);
                }
                if (title.contains("Too many requests") || title.contains("Access denied")) {
                    throw new IOException("GitHub rate limited or access denied for: " + username);
                }
                log.info("GitHub profile fetched: username={}, attempt={}, title={}", username, attempt, title);
                return doc;
            } catch (IOException e) {
                lastEx = e;
                if (attempt < MAX_RETRIES) {
                    // 短退避：原指数退避(1s/2s/4s)与并行总超时不兼容，改成线性 500ms 起步
                    long backoff = attempt * 500L;
                    log.warn("GitHub profile fetch attempt {}/{} failed, retrying in {}ms: {}", attempt, MAX_RETRIES, backoff, e.getMessage());
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted during retry backoff", ie);
                    }
                }
            }
        }
        throw new IOException("Failed to fetch GitHub profile after " + MAX_RETRIES + " attempts", lastEx);
    }

    // 多选择器降级解析贡献热力图（爬虫降级路径用）
    private Map<String, Object> parseContributions(Document doc) {
        Map<String, Object> result = new HashMap<>();

        Elements rects = doc.select(".js-calendar-graph-svg rect.ContributionCalendar-day");
        if (rects.isEmpty()) rects = doc.select("rect.ContributionCalendar-day[data-date]");
        if (rects.isEmpty()) rects = doc.select("svg.js-calendar-graph-svg rect[data-date]");
        if (rects.isEmpty()) rects = doc.select("g rect[data-date][data-level]");
        if (rects.isEmpty()) rects = doc.select("[data-date][data-level]");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<LocalDate, Map<String, Object>> dateMap = new LinkedHashMap<>();
        int totalContributions = 0;

        for (Element rect : rects) {
            String dateStr = rect.attr("data-date");
            String countStr = rect.attr("data-count");
            String levelStr = rect.attr("data-level");
            if (dateStr.isEmpty()) continue;

            LocalDate date = LocalDate.parse(dateStr, fmt);
            int count = countStr.isEmpty() ? 0 : Integer.parseInt(countStr);
            int level = levelStr.isEmpty() ? 0 : Integer.parseInt(levelStr);
            totalContributions += count;

            Map<String, Object> day = new HashMap<>();
            day.put("date", dateStr);
            day.put("count", count);
            day.put("level", level);
            dateMap.put(date, day);
        }

        List<List<Map<String, Object>>> heatmap = new ArrayList<>();
        if (!dateMap.isEmpty()) {
            LocalDate firstDate = dateMap.keySet().iterator().next();
            LocalDate lastDate = firstDate;
            for (LocalDate d : dateMap.keySet()) lastDate = d;

            LocalDate weekStart = firstDate;
            while (weekStart.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
                weekStart = weekStart.minusDays(1);
            }
            while (!weekStart.isAfter(lastDate)) {
                List<Map<String, Object>> week = new ArrayList<>();
                for (int d = 0; d < 7; d++) {
                    LocalDate date = weekStart.plusDays(d);
                    Map<String, Object> dayData = dateMap.get(date);
                    if (dayData == null) {
                        dayData = new HashMap<>();
                        dayData.put("date", date.format(fmt));
                        dayData.put("count", 0);
                        dayData.put("level", 0);
                    }
                    week.add(dayData);
                }
                heatmap.add(week);
                weekStart = weekStart.plusWeeks(1);
            }
        }

        result.put("heatmap", heatmap);
        result.put("total", totalContributions);
        return result;
    }

    private List<GitHubRepoDTO> parseRepos(JsonNode reposArray) {
        List<GitHubRepoDTO> repos = new ArrayList<>();
        if (reposArray != null && reposArray.isArray()) {
            for (JsonNode repo : reposArray) {
                if (repo.path("private").asBoolean(false)) continue;
                GitHubRepoDTO dto = new GitHubRepoDTO();
                dto.setName(repo.path("name").asText());
                dto.setDescription(repo.path("description").asText(null));
                dto.setLanguage(repo.path("language").asText(null));
                dto.setHtmlUrl(repo.path("html_url").asText());
                dto.setStargazersCount(repo.path("stargazers_count").asInt());
                dto.setForksCount(repo.path("forks_count").asInt());
                repos.add(dto);
            }
        }
        return repos;
    }

    private List<GitHubAchievementDTO> parseAchievements(Document doc) {
        List<GitHubAchievementDTO> achievements = new ArrayList<>();
        if (doc == null) return achievements;

        Elements badges = doc.select(
                "img.achievement-badge-sidebar, " +
                "details img[alt*='achievement'], " +
                ".js-achievement-card img, " +
                "img[src*='achievement'], " +
                "img[src*='Achievement'], " +
                "img[data-ga-click*='achievement']");
        if (badges.isEmpty()) {
            badges = doc.select("img[alt*='chieve'], img[src*='chieve']");
        }
        for (Element img : badges) {
            GitHubAchievementDTO dto = new GitHubAchievementDTO();
            dto.setName(img.attr("alt"));
            dto.setIconUrl(img.attr("src"));
            dto.setDescription("");
            if (!dto.getName().isEmpty()) {
                achievements.add(dto);
            }
        }
        return achievements;
    }

    private SSLContext createTrustAllSslContext() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            return sc;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create trust-all SSLContext", e);
        }
    }
}
