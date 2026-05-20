package com.murasame.util;

import com.murasame.entity.Users;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class AuthHelper {

    private final JwtUtil jwtUtil;

    public AuthHelper(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // 获取当前用户：优先 JWT，回退 Session（过渡期双轨运行）
    public Users getCurrentUser(HttpServletRequest request) {
        Users user = getCurrentUserFromJwt(request);
        if (user != null) return user;

        HttpSession session = request.getSession(false);
        if (session != null) {
            return (Users) session.getAttribute("currentUser");
        }
        return null;
    }

    // 仅从 JWT 获取用户（用于纯 API 端点）
    public Users getCurrentUserFromJwt(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.isValidToken(token) && jwtUtil.isAccessToken(token)) {
                var claims = jwtUtil.parseToken(token);
                Users user = new Users();
                user.setId(claims.get("userId", Long.class));
                user.setEmail(claims.get("email", String.class));
                user.setNickname(claims.get("nickname", String.class));
                return user;
            }
        }
        return null;
    }
}
