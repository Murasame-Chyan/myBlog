package com.murasame.config;

import com.murasame.entity.Users;
import com.murasame.service.UserService;
import com.murasame.util.CookieUtil;
import com.murasame.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final UserService userService;
    private final CookieUtil cookieUtil;
    private final JwtProperties jwtProperties;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, StringRedisTemplate redisTemplate,
                                   UserService userService, CookieUtil cookieUtil,
                                   JwtProperties jwtProperties) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.userService = userService;
        this.cookieUtil = cookieUtil;
        this.jwtProperties = jwtProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractFromHeader(request);
        if (token == null) {
            token = extractFromCookie(request);
        }

        if (token == null) {
            log.debug("No JWT found for request: {} {}", request.getMethod(), request.getRequestURI());
        } else if (!jwtUtil.isValidToken(token)) {
            log.debug("Invalid JWT for request: {} {}", request.getMethod(), request.getRequestURI());
        } else if (!jwtUtil.isAccessToken(token)) {
            log.debug("Non-access token for request: {} {}", request.getMethod(), request.getRequestURI());
        }

        if (token != null && jwtUtil.isValidToken(token) && jwtUtil.isAccessToken(token)) {
            String jti = jwtUtil.getJti(token);
            if (jti != null && Boolean.TRUE.equals(redisTemplate.hasKey("jwt:blacklist:" + jti))) {
                log.debug("Token jti={} is blacklisted", jti);
                filterChain.doFilter(request, response);
                return;
            }

            Claims claims = jwtUtil.parseToken(token);
            // JJWT 序列化时小数值可能存为 Integer，统一按 Number 转换避免类型不匹配
            Long userId = null;
            Object uid = claims.get("userId");
            if (uid instanceof Number) {
                userId = ((Number) uid).longValue();
            }
            // 从数据库加载完整用户信息，确保头像、简介、性别、GitHub 等字段可用
            Users user = userId != null ? userService.getUserById(userId) : null;

            if (user != null) {
                request.setAttribute("currentUser", user);

                // 将认证写入 SecurityContext，使 Spring Security URL 规则（authenticated()）生效
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        user, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);

                log.debug("JWT authenticated: userId={}", user.getId());
            }
        }

        // 透明续期：access_token 无效但 refresh_token 有效时，自动签发新 token 对
        if (request.getAttribute("currentUser") == null) {
            trySilentRefresh(request, response);
        }

        filterChain.doFilter(request, response);
    }

    // 尝试用 refresh_token 静默续期，成功则设置 currentUser 和 SecurityContext
    private void trySilentRefresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null || !jwtUtil.isValidToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            return;
        }

        String rtJti = jwtUtil.getJti(refreshToken);
        if (rtJti == null) return;

        Long userId = null;

        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey("jwt:blacklist:" + rtJti))) {
                // 已被拉黑 → 可能是并发请求，尝试读取 refresh-result
                String cached = redisTemplate.opsForValue().get("jwt:refresh-result:" + rtJti);
                if (cached != null) {
                    try {
                        userId = Long.parseLong(cached);
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else {
                // 未被拉黑 → 执行续期
                userId = jwtUtil.getUserIdFromToken(refreshToken);
                if (userId != null) {
                    // 先存结果（供并发请求回退读取）
                    redisTemplate.opsForValue().set(
                            "jwt:refresh-result:" + rtJti,
                            userId.toString(),
                            Duration.ofSeconds(30));
                    // 再拉黑旧 refresh_token
                    long remaining = jwtUtil.getRemainingMillis(refreshToken);
                    if (remaining > 0) {
                        redisTemplate.opsForValue().set(
                                "jwt:blacklist:" + rtJti, "1",
                                Duration.ofMillis(remaining));
                    }
                    // 签发新 token 对
                    Users user = userService.getUserById(userId);
                    if (user != null) {
                        String newAT = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getNickname());
                        String newRT = jwtUtil.generateRefreshToken(user.getId());
                        cookieUtil.writeAccessToken(response, newAT, (int) (jwtProperties.getAccessTokenExpiration() / 1000));
                        cookieUtil.writeRefreshToken(response, newRT, (int) (jwtProperties.getRefreshTokenExpiration() / 1000));
                        log.debug("JWT silent refresh: userId={}", user.getId());
                    }
                }
            }
        } catch (Exception e) {
            // Redis 不可用时降级：跳过续期，用户需手动登录
            log.warn("Silent refresh failed (Redis may be unavailable): {}", e.getMessage());
            return;
        }

        // 认证用户
        if (userId != null) {
            try {
                Users user = userService.getUserById(userId);
                if (user != null) {
                    request.setAttribute("currentUser", user);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            user, null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                log.warn("Failed to load user during silent refresh: {}", e.getMessage());
            }
        }
    }

    private String extractFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private String extractFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (ACCESS_TOKEN_COOKIE.equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (REFRESH_TOKEN_COOKIE.equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }
}
