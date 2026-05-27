package com.murasame.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;

@Component
public class GitHubApiClient {

    // 单次 GitHub HTTP 调用超时：连接 5s + 读取 10s。
    // 原先 new RestTemplate() 默认无限超时，GitHub 抖动时请求会一直挂着，
    // 配合 60s 的 Nginx proxy_read_timeout 必然 502。
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${github.token:}")
    private String token;

    @Value("${github.disable-ssl-verification:false}")
    private boolean disableSslVerification;

    private static final String GITHUB_API = "https://api.github.com";
    private static final String GITHUB_URL = "https://github.com";

    public GitHubApiClient() {
        this.restTemplate = buildRestTemplate(false);
    }

    private RestTemplate getRestTemplate() {
        if (disableSslVerification) {
            return buildRestTemplate(true);
        }
        return restTemplate;
    }

    // 统一构建带超时的 RestTemplate；trustAll=true 时禁用 SSL 校验
    private RestTemplate buildRestTemplate(boolean trustAll) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT);
        if (trustAll) {
            builder.sslContext(createTrustAllSslContext());
        }
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(builder.build());
        factory.setReadTimeout(READ_TIMEOUT);
        return new RestTemplate(factory);
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

    // 获取 GitHub Profile 页面 HTML（成就徽章从此页面解析）
    public Document fetchProfilePage(String username) throws Exception {
        String url = GITHUB_URL + "/" + username;
        var conn = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; myBlog/1.0)")
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "en-US,en;q=0.9")
                .timeout(20000);
        if (disableSslVerification) {
            conn.sslContext(createTrustAllSslContext());
        }
        return conn.get();
    }

    // 解析实际使用的 token：优先使用用户个人 token，其次系统全局 token
    private String resolveToken(String userToken) {
        if (userToken != null && !userToken.isBlank()) {
            return userToken;
        }
        return (token != null && !token.isBlank()) ? token : null;
    }

    // 通过 GitHub GraphQL API 获取贡献日历（支持用户个人 token）
    public JsonNode fetchContributionCalendar(String username, String userToken) throws Exception {
        String effectiveToken = resolveToken(userToken);
        if (effectiveToken == null) {
            throw new IllegalStateException("GitHub token 未配置。请设置环境变量 GITHUB_TOKEN 或在个人资料中绑定 GitHub Token。");
        }
        String query = "{\"query\":\"query($username:String!){user(login:$username){contributionsCollection{contributionCalendar{totalContributions weeks{contributionDays{date contributionCount color}}}}}}\",\"variables\":{\"username\":\"" + username + "\"}}";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + effectiveToken);
        headers.set("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(query, headers);
        ResponseEntity<String> response = getRestTemplate().exchange(
                GITHUB_API + "/graphql", HttpMethod.POST, entity, String.class);
        return objectMapper.readTree(response.getBody());
    }

    // 获取用户仓库列表（REST API，支持用户个人 token）
    public JsonNode listUserRepos(String username, int page, int perPage, String userToken) throws Exception {
        String url = GITHUB_API + "/users/" + username + "/repos?sort=updated&per_page=" + perPage + "&page=" + page;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        String effectiveToken = resolveToken(userToken);
        if (effectiveToken != null) {
            headers.setBearerAuth(effectiveToken);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = getRestTemplate().exchange(url, HttpMethod.GET, entity, String.class);
        return objectMapper.readTree(response.getBody());
    }
}
