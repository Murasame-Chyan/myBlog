package com.murasame.config;

import com.murasame.entity.Users;
import com.murasame.service.UserService;
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
import java.util.Collections;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String ACCESS_TOKEN_COOKIE = "access_token";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, StringRedisTemplate redisTemplate,
                                   UserService userService) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.userService = userService;
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

        filterChain.doFilter(request, response);
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
}
