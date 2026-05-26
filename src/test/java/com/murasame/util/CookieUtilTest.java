package com.murasame.util;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class CookieUtilTest {

    @Test
    void shouldWriteAccessTokenCookieWithRootPath() {
        var resp = new MockHttpServletResponse();
        CookieUtil util = new CookieUtil(false);
        util.writeAccessToken(resp, "token-value", 1800);

        Cookie c = resp.getCookie("access_token");
        assertNotNull(c);
        assertEquals("token-value", c.getValue());
        assertEquals("/", c.getPath());
        assertTrue(c.isHttpOnly());
        assertEquals(1800, c.getMaxAge());
        assertFalse(c.getSecure());
    }

    @Test
    void shouldWriteRefreshTokenCookieWithRefreshPath() {
        var resp = new MockHttpServletResponse();
        CookieUtil util = new CookieUtil(false);
        util.writeRefreshToken(resp, "refresh-value", 604800);

        Cookie c = resp.getCookie("refresh_token");
        assertNotNull(c);
        assertEquals("/auth/refresh", c.getPath());
        assertTrue(c.isHttpOnly());
        assertEquals(604800, c.getMaxAge());
    }

    @Test
    void shouldHonorSecureFlagInProdMode() {
        var resp = new MockHttpServletResponse();
        CookieUtil util = new CookieUtil(true);
        util.writeAccessToken(resp, "x", 1800);

        assertTrue(resp.getCookie("access_token").getSecure());
    }

    @Test
    void shouldClearAccessTokenCookie() {
        var resp = new MockHttpServletResponse();
        CookieUtil util = new CookieUtil(false);
        util.clearAccessToken(resp);

        Cookie c = resp.getCookie("access_token");
        assertNotNull(c);
        assertEquals(0, c.getMaxAge());
        assertEquals("", c.getValue());
    }

    @Test
    void shouldClearRefreshTokenCookieAtCorrectPath() {
        var resp = new MockHttpServletResponse();
        CookieUtil util = new CookieUtil(false);
        util.clearRefreshToken(resp);

        Cookie c = resp.getCookie("refresh_token");
        assertNotNull(c);
        assertEquals(0, c.getMaxAge());
        assertEquals("/auth/refresh", c.getPath());
    }
}
