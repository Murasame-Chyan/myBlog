package com.murasame.config;

import com.murasame.util.JwtUtil;
import com.murasame.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationFilterTest {

    private JwtProperties props;
    private JwtUtil jwtUtil;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        props = new JwtProperties();
        props.setSecret("this-is-a-test-secret-that-is-at-least-256-bits-long-for-hs256!!");
        jwtUtil = new JwtUtil(props);
        filter = new JwtAuthenticationFilter(jwtUtil);
    }

    @Test
    void shouldPassThroughWithoutAuthHeader() throws Exception {
        var req = new MockHttpServletRequest();
        var res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, (r1, r2) -> {});
        assertNotEquals(401, res.getStatus());
    }

    @Test
    void shouldSetUserAttributeForValidToken() throws Exception {
        String token = jwtUtil.generateAccessToken(1L, "t@t.com", "User");
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        var res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, (r1, r2) -> {
            Users u = (Users) r1.getAttribute("currentUser");
            assertNotNull(u);
            assertEquals(1L, u.getId());
        });
    }

    @Test
    void shouldNotSetUserForInvalidToken() throws Exception {
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer invalid.token.here");
        var res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, (r1, r2) -> {
            assertNull(r1.getAttribute("currentUser"));
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
        });
    }

    @Test
    void shouldSkipNonBearerHeader() throws Exception {
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Basic dGVzdDp0ZXN0");
        var res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, (r1, r2) -> {
            assertNull(r1.getAttribute("currentUser"));
        });
    }
}
