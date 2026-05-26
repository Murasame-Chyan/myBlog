package com.murasame.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String REFRESH_TOKEN_PATH = "/auth/refresh";

    private final boolean secure;

    public CookieUtil(@Value("${security.cookie.secure:false}") boolean secure) {
        this.secure = secure;
    }

    public void writeAccessToken(HttpServletResponse response, String token, int maxAgeSeconds) {
        write(response, ACCESS_TOKEN, token, "/", maxAgeSeconds);
    }

    public void writeRefreshToken(HttpServletResponse response, String token, int maxAgeSeconds) {
        write(response, REFRESH_TOKEN, token, REFRESH_TOKEN_PATH, maxAgeSeconds);
    }

    public void clearAccessToken(HttpServletResponse response) {
        write(response, ACCESS_TOKEN, "", "/", 0);
    }

    public void clearRefreshToken(HttpServletResponse response) {
        write(response, REFRESH_TOKEN, "", REFRESH_TOKEN_PATH, 0);
    }

    private void write(HttpServletResponse response, String name, String value, String path, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath(path);
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setAttribute("SameSite", path.equals("/") ? "Lax" : "Strict");
        response.addCookie(cookie);
    }
}
