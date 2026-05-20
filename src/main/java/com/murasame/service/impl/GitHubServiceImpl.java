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

@Service
public class GitHubServiceImpl implements GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubServiceImpl.class);
    private static final String REDIS_KEY_PREFIX = "github:profile:";
    private static final Duration REDIS_TTL = Duration.ofHours(2);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_RETRIES = 3;

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
        // 1. 查 Redis 缓存（不同用户的 token 可能不同，但热力图数据是公开的，按 username 缓存即可）
        String redisKey = REDIS_KEY_PREFIX + username;
        String cached = stringRedisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, GitHubProfileVO.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize cached GitHub profile, re-fetching: {}", e.getMessage());
            }
        }

        GitHubProfileVO vo = new GitHubProfileVO();
        boolean heatmapOk = false;

        // 2. 热力图：优先使用 GraphQL API，降级到页面爬虫
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

            vo.setTotalContributions(total);
            vo.setHeatmap(heatmap);
            heatmapOk = !heatmap.isEmpty();
            log.info("GitHub GraphQL 热力图获取成功: username={}, total={}, weeks={}", username, total, heatmap.size());
        } catch (IllegalStateException e) {
            log.warn("GitHub GraphQL API 不可用，降级到页面爬虫: {}", e.getMessage());
            heatmapOk = fetchHeatmapFromPage(vo, username);
        } catch (Exception e) {
            log.warn("GitHub GraphQL API 失败，降级到页面爬虫 (username={}): {}", username, e.getMessage());
            heatmapOk = fetchHeatmapFromPage(vo, username);
        }

        // 3. 成就徽章（仍从 Profile 页面解析）
        try {
            Document profileDoc = fetchProfileWithRetry(username);
            vo.setAchievements(parseAchievements(profileDoc));
        } catch (Exception e) {
            log.warn("获取 GitHub 成就失败 (username={}): {}", username, e.getMessage());
            vo.setAchievements(new ArrayList<>());
        }

        // 4. 仓库列表（REST API，传入用户 token 提高限流额度）
        try {
            vo.setRepos(parseRepos(apiClient.listUserRepos(username, 1, 10, userToken)));
        } catch (Exception e) {
            log.warn("获取 GitHub 仓库列表失败 (username={}): {}", username, e.getMessage());
            vo.setRepos(new ArrayList<>());
        }

        // 5. 仅在成功获取到热力图时才写入 Redis
        if (heatmapOk && vo.getHeatmap() != null && !vo.getHeatmap().isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(vo);
                stringRedisTemplate.opsForValue().set(redisKey, json, REDIS_TTL);
                log.info("GitHub profile cached in Redis: username={}, contributions={}", username, vo.getTotalContributions());
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize GitHub profile for cache: {}", e.getMessage());
            }
        } else {
            log.warn("GitHub profile NOT cached (empty heatmap): username={}, heatmapOk={}", username, heatmapOk);
        }

        return vo;
    }

    private boolean fetchHeatmapFromPage(GitHubProfileVO vo, String username) {
        try {
            Document doc = fetchProfileWithRetry(username);
            Map<String, Object> cr = parseContributions(doc);
            vo.setTotalContributions((int) cr.get("total"));
            @SuppressWarnings("unchecked")
            List<List<Map<String, Object>>> heatmap = (List<List<Map<String, Object>>>) cr.get("heatmap");
            vo.setHeatmap(heatmap);
            return heatmap != null && !heatmap.isEmpty();
        } catch (Exception e) {
            log.warn("页面爬虫热力图也失败 (username={}): {}", username, e.getMessage());
            vo.setTotalContributions(0);
            vo.setHeatmap(new ArrayList<>());
            return false;
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
                        .timeout(25000);
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
                    long backoff = (long) Math.pow(2, attempt) * 1000;
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
