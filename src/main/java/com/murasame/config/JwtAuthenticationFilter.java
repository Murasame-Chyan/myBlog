package com.murasame.config;

import com.murasame.entity.Users;
import com.murasame.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        // 优先从 HttpOnly cookie 读取 JWT，其次从 Authorization header
        String token = extractTokenFromCookie(request);
        if (token == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (token != null && jwtUtil.isValidToken(token) && jwtUtil.isAccessToken(token)) {
            Claims claims = jwtUtil.parseToken(token);
            Users user = new Users();
            user.setId(claims.get("userId", Long.class));
            user.setEmail(claims.get("email", String.class));
            user.setNickname(claims.get("nickname", String.class));
            request.setAttribute("currentUser", user);
            // 同步写入 Session，确保 Thymeleaf 模板和 UserInterceptor 能读取
            request.getSession(true).setAttribute("currentUser", user);
            log.debug("JWT authenticated: userId={}", user.getId());
        } else if (token == null) {
            // 无 JWT 时从 Session 恢复 request attribute，供 AuthHelper 三轨检查
            var session = request.getSession(false);
            if (session != null) {
                var user = session.getAttribute("currentUser");
                if (user != null) {
                    request.setAttribute("currentUser", user);
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("access_token".equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }
}
