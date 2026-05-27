package com.murasame.config;

import com.murasame.entity.Users;
import com.murasame.service.UserService;
import com.murasame.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtProperties props;
    private JwtUtil jwtUtil;
    private JwtAuthenticationFilter filter;
    private StringRedisTemplate redisTemplate;
    private UserService userService;

    @BeforeEach
    void setUp() {
        props = new JwtProperties();
        props.setSecret("this-is-a-test-secret-that-is-at-least-256-bits-long-for-hs256!!");
        props.setAccessTokenExpiration(3600000); // 1 hour so tokens don't expire during test
        jwtUtil = new JwtUtil(props);
        redisTemplate = mock(StringRedisTemplate.class);
        userService = mock(UserService.class);
        // 默认任何 key 都不在黑名单中
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        filter = new JwtAuthenticationFilter(jwtUtil, redisTemplate, userService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPassThroughWithoutAuthHeader() throws Exception {
        var req = new MockHttpServletRequest();
        var res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, (r1, r2) -> {});
        assertNotEquals(401, res.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldSetUserAttributeAndSecurityContextForValidHeader() throws Exception {
        String token = jwtUtil.generateAccessToken(1L, "t@t.com", "User");
        Users mockUser = new Users();
        mockUser.setId(1L);
        mockUser.setEmail("t@t.com");
        mockUser.setNickname("User");
        when(userService.getUserById(anyLong())).thenReturn(mockUser);

        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        var res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, (r1, r2) -> {
            Users u = (Users) r1.getAttribute("currentUser");
            assertNotNull(u);
            assertEquals(1L, u.getId());
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertTrue(auth.isAuthenticated());
            assertSame(u, auth.getPrincipal());
        });
    }

    @Test
    void shouldExtractTokenFromCookie() throws Exception {
        String token = jwtUtil.generateAccessToken(2L, "c@c.com", "Cookie");
        Users mockUser = new Users();
        mockUser.setId(2L);
        when(userService.getUserById(anyLong())).thenReturn(mockUser);

        var req = new MockHttpServletRequest();
        req.setCookies(new Cookie("access_token", token));
        var res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, (r1, r2) -> {
            Users u = (Users) r1.getAttribute("currentUser");
            assertNotNull(u);
            assertEquals(2L, u.getId());
        });
    }

    @Test
    void shouldPreferHeaderOverCookie() throws Exception {
        String headerToken = jwtUtil.generateAccessToken(10L, "h@h.com", "Header");
        String cookieToken = jwtUtil.generateAccessToken(20L, "c@c.com", "Cookie");
        Users mockUser = new Users();
        mockUser.setId(10L);
        when(userService.getUserById(anyLong())).thenReturn(mockUser);

        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + headerToken);
        req.setCookies(new Cookie("access_token", cookieToken));
        var res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, (r1, r2) -> {
            Users u = (Users) r1.getAttribute("currentUser");
            assertEquals(10L, u.getId());
        });
    }

    @Test
    void shouldNotSetUserForInvalidToken() throws Exception {
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer invalid.token.here");
        var res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, (r1, r2) -> {
            assertNull(r1.getAttribute("currentUser"));
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        });
    }

    @Test
    void shouldNotSetUserForRefreshToken() throws Exception {
        String token = jwtUtil.generateRefreshToken(1L);
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        var res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, (r1, r2) -> {
            assertNull(r1.getAttribute("currentUser"));
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        });
    }

    @Test
    void shouldNotCreateSession() throws Exception {
        String token = jwtUtil.generateAccessToken(1L, "t@t.com", "User");
        Users mockUser = new Users();
        mockUser.setId(1L);
        when(userService.getUserById(anyLong())).thenReturn(mockUser);

        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        var res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, (r1, r2) -> {});
        assertNull(req.getSession(false), "Filter must not create or write to session");
    }
}
