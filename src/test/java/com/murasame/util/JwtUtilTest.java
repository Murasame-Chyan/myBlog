package com.murasame.util;

import com.murasame.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private JwtProperties props;

    @BeforeEach
    void setUp() {
        props = new JwtProperties();
        props.setSecret("this-is-a-test-secret-that-is-at-least-256-bits-long-for-hs256!!");
        props.setAccessTokenExpiration(1_800_000);
        props.setRefreshTokenExpiration(604_800_000);
        jwtUtil = new JwtUtil(props);
    }

    @Test
    void shouldGenerateValidAccessToken() {
        String token = jwtUtil.generateAccessToken(1L, "test@example.com", "TestUser");
        assertNotNull(token);
        Claims claims = jwtUtil.parseToken(token);
        assertEquals(1L, claims.get("userId", Long.class));
        assertEquals("test@example.com", claims.get("email", String.class));
        assertEquals("TestUser", claims.get("nickname", String.class));
        assertEquals("access", claims.get("type", String.class));
    }

    @Test
    void shouldGenerateValidRefreshToken() {
        String token = jwtUtil.generateRefreshToken(1L);
        assertNotNull(token);
        Claims claims = jwtUtil.parseToken(token);
        assertEquals(1L, claims.get("userId", Long.class));
        assertEquals("refresh", claims.get("type", String.class));
    }

    @Test
    void shouldDetectExpiredToken() {
        props.setAccessTokenExpiration(1); // 1ms
        JwtUtil shortLived = new JwtUtil(props);
        String token = shortLived.generateAccessToken(1L, "e@e.com", "u");
        assertTrue(shortLived.isTokenExpired(token));
    }

    @Test
    void shouldRejectInvalidToken() {
        assertTrue(jwtUtil.isTokenExpired("invalid.token.here"));
        assertNull(jwtUtil.parseToken("invalid.token.here"));
    }

    @Test
    void shouldGetUserIdFromToken() {
        String token = jwtUtil.generateAccessToken(42L, "e@e.com", "u");
        assertEquals(42L, jwtUtil.getUserIdFromToken(token));
    }

    @Test
    void shouldDistinguishAccessAndRefreshTokens() {
        String access = jwtUtil.generateAccessToken(1L, "e@e.com", "u");
        String refresh = jwtUtil.generateRefreshToken(1L);
        assertFalse(jwtUtil.isRefreshToken(access));
        assertTrue(jwtUtil.isRefreshToken(refresh));
        assertTrue(jwtUtil.isAccessToken(access));
        assertFalse(jwtUtil.isAccessToken(refresh));
    }

    @Test
    void shouldReturnTokenIssuedAt() {
        String token = jwtUtil.generateAccessToken(1L, "e@e.com", "u");
        assertNotNull(jwtUtil.getIssuedAt(token));
    }
}
