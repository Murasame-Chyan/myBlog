package com.murasame.controller;

import com.murasame.config.TencentCosConfig;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;

@Slf4j
@RestController
public class ImageProxyController {

    private static final int MAX_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String[] BLOCKED_NETS = {"127.", "10.", "0.", "169.254.", "192.168."};

    private final String allowedHost;

    public ImageProxyController(TencentCosConfig cosConfig) {
        String baseUrl = cosConfig.getBaseUrl();
        this.allowedHost = (baseUrl != null) ? URI.create(baseUrl).getHost() : null;
        log.info("ImageProxy whitelist host: {}", allowedHost);
    }

    @GetMapping("/api/proxy-image")
    public void proxy(@RequestParam String url, HttpServletResponse response) {
        if (allowedHost == null) {
            log.warn("Proxy blocked: COS baseUrl not configured");
            response.setStatus(502);
            return;
        }
        if (url == null || url.isBlank()) {
            response.setStatus(400);
            return;
        }
        if (!url.startsWith("https://")) {
            log.warn("Proxy blocked: non-HTTPS URL {}", url);
            response.setStatus(403);
            return;
        }

        URI targetUri;
        try {
            targetUri = new URI(url);
        } catch (Exception e) {
            log.warn("Proxy blocked: invalid URL {}", url);
            response.setStatus(400);
            return;
        }

        String host = targetUri.getHost();
        if (host == null || !host.equals(allowedHost)) {
            log.warn("Proxy blocked: host {} not allowed", host);
            response.setStatus(403);
            return;
        }
        if (isPrivateHost(host)) {
            log.warn("Proxy blocked: private IP {}", host);
            response.setStatus(403);
            return;
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) targetUri.toURL().openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "MyBlog-ImageProxy/1.0");
            conn.setInstanceFollowRedirects(false);
            conn.connect();

            int status = conn.getResponseCode();
            if (status >= 300 && status < 400) {
                log.warn("Proxy blocked: COS returned redirect {} {}", status, conn.getHeaderField("Location"));
                response.setStatus(502);
                conn.disconnect();
                return;
            }

            String contentType = conn.getContentType();
            if (contentType != null) response.setContentType(contentType);
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Cache-Control", "public, max-age=86400");

            long contentLength = conn.getContentLengthLong();
            if (contentLength > MAX_SIZE) {
                log.warn("Proxy blocked: oversize {}MB", contentLength / 1024 / 1024);
                response.setStatus(413);
                conn.disconnect();
                return;
            }

            byte[] buf = new byte[8192];
            int total = 0;
            try (InputStream in = conn.getInputStream()) {
                int n;
                while ((n = in.read(buf)) != -1) {
                    total += n;
                    if (total > MAX_SIZE) {
                        log.warn("Proxy blocked: stream exceeded limit");
                        response.setStatus(413);
                        return;
                    }
                    response.getOutputStream().write(buf, 0, n);
                }
                response.getOutputStream().flush();
            }
            conn.disconnect();
        } catch (Exception e) {
            log.error("Proxy failed: {}", url, e);
            response.setStatus(502);
        }
    }

    private boolean isPrivateHost(String host) {
        try {
            InetAddress addr = InetAddress.getByName(host);
            byte[] octets = addr.getAddress();
            if (octets.length == 4) {
                int a = octets[0] & 0xFF, b = octets[1] & 0xFF;
                if (a == 127 || a == 10 || a == 0) return true;
                if (a == 172 && b >= 16 && b <= 31) return true;
                if (a == 192 && b == 168) return true;
                if (a == 169 && b == 254) return true;
            }
            return addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress();
        } catch (Exception e) {
            // 解析失败则放行（可能是 CDN 域名），信任 URL 白名单
            return false;
        }
    }
}
