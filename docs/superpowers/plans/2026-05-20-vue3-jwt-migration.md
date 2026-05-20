# Vue3 + Element Plus + JWT Auth Migration — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate blog from Thymeleaf server-rendered pages + session auth to Vue 3 MPA + JWT token auth, incrementally with dual-run (session + JWT coexist during transition).

**Architecture:** Backend gains JwtAuthenticationFilter alongside existing UserInterceptor. AuthHelper provides unified user lookup (JWT first, session fallback). Frontend is a Vite multi-entry Vue 3 + Element Plus project built to `static/dist/`. Each page is an independent Vue app. A parallel set of `/api/*` JSON endpoints is added alongside existing Thymeleaf page controllers, so both old and new frontends work during migration.

**Tech Stack:** Java 17, Spring Boot 3.5, jjwt 0.12.6, Vue 3 (Composition API, JS), Vite 6, Element Plus 2.9, Axios, Pinia, marked + DOMPurify

**Key design decisions:**
- **Dual-run auth**: JwtFilter sets `request.setAttribute("currentUser", user)`; AuthHelper checks it; old session code still works
- **Dual-run pages**: Existing Thymeleaf controllers unchanged; new `/api/*` JSON endpoints serve Vue frontend; old pages serve existing users during migration
- **No TypeScript**: Keep JS to reduce friction
- **Sexy UI: no**: "尽可能地用 ElementPlus 简化样式表达" — use Element Plus defaults, minimal custom CSS
- **Weather & GitHub**: Wrap existing API endpoints in slim Vue components; if JS logic is too complex, render server-fragment inside Vue shell via iframe or static embed

---

## File Map

### Backend — New (8 files)
| File | Purpose |
|------|---------|
| `src/main/java/com/murasame/config/JwtProperties.java` | `@ConfigurationProperties("jwt")` |
| `src/main/java/com/murasame/util/JwtUtil.java` | Issue, validate, parse JWT |
| `src/main/java/com/murasame/config/JwtAuthenticationFilter.java` | OncePerRequestFilter |
| `src/main/java/com/murasame/util/AuthHelper.java` | Dual JWT/session user lookup |
| `src/main/java/com/murasame/controller/ApiController.java` | New `/api/*` JSON endpoints for Vue |
| `src/test/java/com/murasame/util/JwtUtilTest.java` | 7 tests for JWT operations |
| `src/test/java/com/murasame/config/JwtAuthenticationFilterTest.java` | 5 tests for filter |
| `src/test/java/com/murasame/controller/AuthControllerTest.java` | Auth endpoint integration tests |

### Backend — Modified (8 files)
| File | Change |
|------|--------|
| `pom.xml` | Add jjwt + spring-boot-starter-test + junit + mockito deps |
| `application.yml` | Add `jwt:` placeholder config |
| `MyBlogApplication.java` | Add `@EnableConfigurationProperties(JwtProperties.class)` |
| `SecurityConfiguration.java` | Register JwtFilter, disable formLogin, add test deps |
| `AuthController.java` | login/register return JWT; add `/auth/refresh`; captcha → captchaToken header; logout writes blacklist |
| `BlogController.java` | Add `/api/blogs/*` JSON endpoints (publish, update, delete already return JSON, just add list/detail) |
| `WebConfiguration.java` | Exclude `/api/**` from UserInterceptor (already done), add `/dist/**` resource handler |
| External `application.properties` | Add `jwt.secret=` |

### Backend — Deleted in cleanup (Phase 9)
| File | Reason |
|------|--------|
| `UserInterceptor.java` | Replaced by JwtAuthenticationFilter |

### Frontend — New (~22 files)
| File | Purpose |
|------|---------|
| `src/main/frontend/package.json` | Dependencies |
| `src/main/frontend/vite.config.js` | Multi-entry build |
| `src/main/frontend/src/utils/token.js` | localStorage token helpers |
| `src/main/frontend/src/utils/request.js` | Axios + JWT interceptor |
| `src/main/frontend/src/utils/markdown.js` | marked + DOMPurify |
| `src/main/frontend/src/stores/auth.js` | Pinia auth store |
| `src/main/frontend/src/shared/styles/global.scss` | Global styles |
| `src/main/frontend/src/shared/components/AppHeader.vue` | Nav + auth state |
| `src/main/frontend/src/shared/components/AuthModal.vue` | Login/register/reset modal |
| `src/main/frontend/src/shared/components/EditProfileModal.vue` | Edit profile modal |
| `src/main/frontend/src/shared/components/CommentSection.vue` | Comment tree |
| `src/main/frontend/src/shared/components/WeatherWidget.vue` | Weather wrapper |
| `src/main/frontend/src/pages/index/index.html` | Vite entry |
| `src/main/frontend/src/pages/index/main.js` | Vue mount |
| `src/main/frontend/src/pages/index/IndexPage.vue` | Blog list page |
| `src/main/frontend/src/pages/readBlog/index.html` + `main.js` + `ReadBlogPage.vue` | Blog detail |
| `src/main/frontend/src/pages/writeBlog/index.html` + `main.js` + `WriteBlogPage.vue` | Write/edit |
| `src/main/frontend/src/pages/profile/index.html` + `main.js` + `ProfilePage.vue` | Profile |
| `src/main/frontend/src/pages/archives/index.html` + `main.js` + `ArchivesPage.vue` | Archives |
| `src/main/frontend/src/pages/followList/index.html` + `main.js` + `FollowListPage.vue` | Follow list |
| `src/main/frontend/src/pages/error/index.html` + `main.js` + `ErrorPage.vue` | Error page |

### Thymeleaf Templates — Modified (8+ files) in Phase 8
Each becomes a thin shell: `<div id="app"></div>` + `<script>` tags + optional `window.__INITIAL_STATE__`.

---

## Phase 1: Backend JWT Infrastructure

### Task 1.1: Add dependencies

**Files:** Modify `pom.xml`

- [ ] **Step 1: Add jjwt + test dependencies to pom.xml**

Find the `<dependencies>` section. Add these after the last `<dependency>`:

```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- Test dependencies (src/test does not exist yet, this bootstraps it) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Create src/test directory structure**

```bash
mkdir -p src/test/java/com/murasame/util
mkdir -p src/test/java/com/murasame/config
mkdir -p src/test/java/com/murasame/controller
mkdir -p src/test/resources
```

- [ ] **Step 3: Verify dependencies resolve**

```bash
mvn dependency:resolve 2>&1 | tail -3
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add pom.xml src/test/
git commit -m "feat: add jjwt and test dependencies, create src/test structure

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 1.2: Create JwtProperties

**Files:** Create `src/main/java/com/murasame/config/JwtProperties.java`

- [ ] **Step 1: Write JwtProperties**

```java
package com.murasame.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenExpiration = 1_800_000;   // 30 min
    private long refreshTokenExpiration = 604_800_000; // 7 days

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getAccessTokenExpiration() { return accessTokenExpiration; }
    public void setAccessTokenExpiration(long accessTokenExpiration) { this.accessTokenExpiration = accessTokenExpiration; }
    public long getRefreshTokenExpiration() { return refreshTokenExpiration; }
    public void setRefreshTokenExpiration(long refreshTokenExpiration) { this.refreshTokenExpiration = refreshTokenExpiration; }
}
```

- [ ] **Step 2: Add @EnableConfigurationProperties to main class**

Read `MyBlogApplication.java`, add the annotation:

```java
package com.murasame;

import com.murasame.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class MyBlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyBlogApplication.class, args);
    }
}
```

- [ ] **Step 3: Add placeholder config**

Append to `src/main/resources/application.yml`:

```yaml
jwt:
  secret: ${JWT_SECRET:}
  access-token-expiration: ${JWT_ACCESS_EXPIRATION:1800000}
  refresh-token-expiration: ${JWT_REFRESH_EXPIRATION:604800000}
```

- [ ] **Step 4: Add real secret to external properties**

Append to `application.properties` (project root):

```properties
# --- JWT ---
jwt.secret=change-me-to-a-real-256-bit-secret
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/murasame/config/JwtProperties.java \
        src/main/java/com/murasame/MyBlogApplication.java \
        src/main/resources/application.yml \
        application.properties
git commit -m "feat: add JwtProperties config class with placeholder values

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 1.3: Create JwtUtil (TDD)

**Files:**
- Create: `src/test/java/com/murasame/util/JwtUtilTest.java`
- Create: `src/main/java/com/murasame/util/JwtUtil.java`

- [ ] **Step 1: Write JwtUtilTest (test first)**

```java
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
        props.setAccessTokenExpiration(1); // 1ms — expires immediately
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
```

- [ ] **Step 2: Run tests — expect FAIL (compilation error, class not created)**

```bash
mvn test -Dtest=JwtUtilTest 2>&1 | tail -10
```

- [ ] **Step 3: Write JwtUtil**

```java
package com.murasame.util;

import com.murasame.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String email, String nickname) {
        Date now = new Date();
        return Jwts.builder()
                .claim("userId", userId)
                .claim("email", email)
                .claim("nickname", nickname)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtProperties.getAccessTokenExpiration()))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .claim("userId", userId)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration()))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.debug("JWT parse failed: {}", e.getMessage());
            return null;
        }
    }

    public boolean isTokenExpired(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return true;
        Date exp = claims.getExpiration();
        return exp != null && exp.before(new Date());
    }

    public boolean isValidToken(String token) {
        return parseToken(token) != null && !isTokenExpired(token);
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        Object uid = claims.get("userId");
        return uid instanceof Number ? ((Number) uid).longValue() : null;
    }

    public boolean isRefreshToken(String token) {
        Claims claims = parseToken(token);
        return claims != null && "refresh".equals(claims.get("type", String.class));
    }

    public boolean isAccessToken(String token) {
        Claims claims = parseToken(token);
        return claims != null && "access".equals(claims.get("type", String.class));
    }

    public Date getIssuedAt(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getIssuedAt() : null;
    }
}
```

- [ ] **Step 4: Run tests — expect PASS**

```bash
mvn test -Dtest=JwtUtilTest
```

Expected: Tests run: 7, Failures: 0.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/murasame/util/JwtUtil.java \
        src/test/java/com/murasame/util/JwtUtilTest.java
git commit -m "feat: add JwtUtil for JWT generation and validation

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 1.4: Create AuthHelper

**Files:** Create `src/main/java/com/murasame/util/AuthHelper.java`

- [ ] **Step 1: Write AuthHelper**

```java
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

    // 获取当前用户：优先 JWT，回退 Session
    public Users getCurrentUser(HttpServletRequest request) {
        Users user = getCurrentUserFromJwt(request);
        if (user != null) return user;

        HttpSession session = request.getSession(false);
        if (session != null) {
            return (Users) session.getAttribute("currentUser");
        }
        return null;
    }

    // 仅从 JWT 获取用户
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
```

- [ ] **Step 2: Build verify**

```bash
mvn clean compile
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/murasame/util/AuthHelper.java
git commit -m "feat: add AuthHelper for dual JWT/session user lookup

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 1.5: Create JwtAuthenticationFilter (TDD)

**Files:**
- Create: `src/test/java/com/murasame/config/JwtAuthenticationFilterTest.java`
- Create: `src/main/java/com/murasame/config/JwtAuthenticationFilter.java`

- [ ] **Step 1: Write JwtAuthenticationFilterTest**

```java
package com.murasame.config;

import com.murasame.util.JwtUtil;
import com.murasame.entity.Users;
import jakarta.servlet.FilterChain;
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
```

- [ ] **Step 2: Run tests — expect FAIL**

```bash
mvn test -Dtest=JwtAuthenticationFilterTest 2>&1 | tail -10
```

- [ ] **Step 3: Write JwtAuthenticationFilter**

```java
package com.murasame.config;

import com.murasame.entity.Users;
import com.murasame.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.isValidToken(token) && jwtUtil.isAccessToken(token)) {
                Claims claims = jwtUtil.parseToken(token);
                Users user = new Users();
                user.setId(claims.get("userId", Long.class));
                user.setEmail(claims.get("email", String.class));
                user.setNickname(claims.get("nickname", String.class));
                request.setAttribute("currentUser", user);
                log.debug("JWT authenticated: userId={}", user.getId());
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 4: Run tests — expect PASS**

```bash
mvn test -Dtest=JwtAuthenticationFilterTest
```

Expected: Tests run: 5, Failures: 0.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/murasame/config/JwtAuthenticationFilter.java \
        src/test/java/com/murasame/config/JwtAuthenticationFilterTest.java
git commit -m "feat: add JwtAuthenticationFilter with tests

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 1.6: Update SecurityConfiguration

**Files:** Modify `src/main/java/com/murasame/config/SecurityConfiguration.java`

- [ ] **Step 1: Rewrite SecurityConfiguration**

```java
package com.murasame.config;

import com.murasame.util.JwtUtil;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@MapperScan("com.murasame.mapper")
public class SecurityConfiguration {

    private final JwtUtil jwtUtil;

    public SecurityConfiguration(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/static/**", "/dist/**").permitAll();
                    auth.requestMatchers("/css/**", "/js/**", "/images/**", "/pics/**").permitAll();
                    auth.anyRequest().permitAll(); // 鉴权交给 JwtFilter + UserInterceptor
                })
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.NEVER))
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 2: Build verify**

```bash
mvn clean compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/murasame/config/SecurityConfiguration.java
git commit -m "feat: register JwtAuthenticationFilter, disable formLogin

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Phase 2: Auth Controller Migration

### Task 2.1: Rewrite AuthController for JWT

**Files:** Modify `src/main/java/com/murasame/controller/AuthController.java`

This is a large rewrite. The complete file content is provided below — copy the entire file.

Three key changes:
1. `login()` — returns `{ accessToken, refreshToken, expiresIn, nickname, avatar }`
2. `captcha()` — uses `X-Captcha-Token` header instead of sessionId
3. New `refresh()` endpoint — exchanges refreshToken for new tokens
4. `logout()` — writes JWT blacklist timestamp to Redis
5. `status()` — reads from JWT Authorization header, falls back to session

**Full new AuthController code** (replaces entire file content):

```java
package com.murasame.controller;

import com.murasame.config.JwtProperties;
import com.murasame.entity.Users;
import com.murasame.service.LoginAttemptService;
import com.murasame.service.MailService;
import com.murasame.service.UserService;
import com.murasame.util.CaptchaUtil;
import com.murasame.util.JwtUtil;
import com.murasame.util.ReturnUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RequestMapping("/auth")
@Controller
@Validated
@Tag(name = "认证接口", description = "登录、注册、登出、刷新令牌")
public class AuthController {

    @Resource
    private UserService userService;

    @Resource
    private LoginAttemptService loginAttemptService;

    @Resource
    private MailService mailService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private JwtProperties jwtProperties;

    @ResponseBody
    @PostMapping("/login")
    public Map<String, Object> login(
            @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确")
            @Size(max = 255) @RequestParam String email,
            @NotBlank(message = "密码不能为空")
            @Size(max = 255) @RequestParam String password,
            @NotBlank(message = "验证码不能为空")
            @RequestParam String captchaCode,
            @NotBlank(message = "验证码令牌不能为空")
            @RequestParam String captchaToken) {

        String lockMsg = loginAttemptService.checkLocked(email);
        if (lockMsg != null) return ReturnUtil.error(lockMsg);

        String captchaKey = "captcha:" + captchaToken;
        String stored = stringRedisTemplate.opsForValue().get(captchaKey);
        if (stored == null) return ReturnUtil.error("验证码已过期，请刷新后重试");
        if (!captchaCode.equalsIgnoreCase(stored)) return ReturnUtil.error("图形验证码错误");
        stringRedisTemplate.delete(captchaKey);

        Users user = userService.login(email, password);
        if (user == null) {
            loginAttemptService.recordFailedAttempt(email);
            return ReturnUtil.error("邮箱或密码错误");
        }

        loginAttemptService.resetAttempts(email);
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getNickname());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        long expiresIn = jwtProperties.getAccessTokenExpiration() / 1000;

        return ReturnUtil.success("登录成功", Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "expiresIn", expiresIn,
                "nickname", user.getNickname() != null ? user.getNickname() : "",
                "avatar", user.getAvatar() != null ? user.getAvatar() : ""
        ));
    }

    @ResponseBody
    @PostMapping("/register")
    public Map<String, Object> register(
            @NotBlank @Email @Size(max = 255) @RequestParam String email,
            @NotBlank @Size(max = 32) @RequestParam String nickname,
            @NotBlank @Size(min = 6, max = 255) @RequestParam String password,
            @NotBlank @RequestParam String emailCode) {

        String emailCodeKey = "email:code:" + email;
        String stored = stringRedisTemplate.opsForValue().get(emailCodeKey);
        if (stored == null) return ReturnUtil.error("邮箱验证码已过期，请重新获取");
        if (!emailCode.equals(stored)) return ReturnUtil.error("邮箱验证码错误");
        stringRedisTemplate.delete(emailCodeKey);

        try {
            Users user = userService.register(email, nickname, password);
            String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getNickname());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId());
            return ReturnUtil.success("注册成功", Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken,
                    "nickname", user.getNickname(),
                    "avatar", user.getAvatar() != null ? user.getAvatar() : ""
            ));
        } catch (IllegalArgumentException e) {
            return ReturnUtil.error(e.getMessage());
        }
    }

    @GetMapping("/captcha")
    public void captcha(HttpServletResponse response) throws IOException {
        String captchaToken = UUID.randomUUID().toString().replace("-", "");
        String code = CaptchaUtil.generateCode();
        stringRedisTemplate.opsForValue().set("captcha:" + captchaToken, code, Duration.ofMinutes(5));
        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setHeader("X-Captcha-Token", captchaToken);
        ImageIO.write(CaptchaUtil.generateImage(code), "png", response.getOutputStream());
    }

    @ResponseBody
    @PostMapping("/send-code")
    public Map<String, Object> sendCode(
            @NotBlank @Email @Size(max = 255) @RequestParam String email) {
        String limitKey = "email:code:limit:" + email;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(limitKey))) {
            Long remaining = stringRedisTemplate.getExpire(limitKey, java.util.concurrent.TimeUnit.SECONDS);
            return ReturnUtil.error("请等待 " + (remaining != null ? remaining : 60) + " 秒后再发送验证码");
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        stringRedisTemplate.opsForValue().set("email:code:" + email, code, Duration.ofMinutes(5));
        stringRedisTemplate.opsForValue().set(limitKey, "1", Duration.ofSeconds(60));
        if (!mailService.sendVerificationCode(email, code)) {
            stringRedisTemplate.delete("email:code:" + email);
            stringRedisTemplate.delete(limitKey);
            return ReturnUtil.error("验证码发送失败，请稍后重试");
        }
        return ReturnUtil.success("验证码已发送");
    }

    @ResponseBody
    @PostMapping("/send-reset-code")
    public Map<String, Object> sendResetCode(
            @NotBlank @Email @Size(max = 255) @RequestParam String email) {
        if (userService.getUserByEmail(email) == null) return ReturnUtil.error("该邮箱未注册");
        String limitKey = "email:reset:limit:" + email;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(limitKey))) {
            Long remaining = stringRedisTemplate.getExpire(limitKey, java.util.concurrent.TimeUnit.SECONDS);
            return ReturnUtil.error("请等待 " + (remaining != null ? remaining : 60) + " 秒后再发送验证码");
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        stringRedisTemplate.opsForValue().set("email:reset:" + email, code, Duration.ofMinutes(5));
        stringRedisTemplate.opsForValue().set(limitKey, "1", Duration.ofSeconds(60));
        if (!mailService.sendVerificationCode(email, code)) {
            stringRedisTemplate.delete("email:reset:" + email);
            stringRedisTemplate.delete(limitKey);
            return ReturnUtil.error("验证码发送失败，请稍后重试");
        }
        return ReturnUtil.success("验证码已发送");
    }

    @ResponseBody
    @PostMapping("/reset-password")
    public Map<String, Object> resetPassword(
            @NotBlank @Email @Size(max = 255) @RequestParam String email,
            @NotBlank @Size(min = 6, max = 255) @RequestParam String newPassword,
            @NotBlank @RequestParam String emailCode) {
        String resetKey = "email:reset:" + email;
        String stored = stringRedisTemplate.opsForValue().get(resetKey);
        if (stored == null) return ReturnUtil.error("邮箱验证码已过期，请重新获取");
        if (!emailCode.equals(stored)) return ReturnUtil.error("邮箱验证码错误");
        stringRedisTemplate.delete(resetKey);
        try {
            userService.resetPassword(email, newPassword);
        } catch (IllegalArgumentException e) {
            return ReturnUtil.error(e.getMessage());
        }
        return ReturnUtil.success("密码重置成功");
    }

    @ResponseBody
    @GetMapping("/status")
    public Map<String, Object> status(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.isValidToken(token) && jwtUtil.isAccessToken(token)) {
                Claims claims = jwtUtil.parseToken(token);
                return ReturnUtil.custom(200, "已登录", Map.of(
                        "loggedIn", true,
                        "nickname", claims.get("nickname", String.class),
                        "avatar", ""
                ));
            }
        }
        var session = request.getSession(false);
        if (session != null) {
            Users user = (Users) session.getAttribute("currentUser");
            if (user != null) {
                return ReturnUtil.custom(200, "已登录", Map.of(
                        "loggedIn", true,
                        "nickname", user.getNickname() != null ? user.getNickname() : "",
                        "avatar", user.getAvatar() != null ? user.getAvatar() : ""
                ));
            }
        }
        return ReturnUtil.custom(200, "未登录", Map.of("loggedIn", false));
    }

    @ResponseBody
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId != null) {
                stringRedisTemplate.opsForValue().set(
                        "jwt:blacklist:" + userId,
                        String.valueOf(System.currentTimeMillis()),
                        Duration.ofMillis(jwtProperties.getRefreshTokenExpiration()));
            }
        }
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        return ReturnUtil.success("已登出");
    }

    @ResponseBody
    @PostMapping("/refresh")
    public Map<String, Object> refresh(@RequestParam String refreshToken) {
        if (!jwtUtil.isValidToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            return ReturnUtil.error("无效的刷新令牌");
        }
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        if (userId == null) return ReturnUtil.error("无效的刷新令牌");

        String blackKey = "jwt:blacklist:" + userId;
        String blackTs = stringRedisTemplate.opsForValue().get(blackKey);
        if (blackTs != null) {
            long bt = Long.parseLong(blackTs);
            var iat = jwtUtil.getIssuedAt(refreshToken);
            if (iat != null && iat.getTime() < bt) return ReturnUtil.error("令牌已被撤销，请重新登录");
        }

        Users user = userService.getUserById(userId);
        if (user == null) return ReturnUtil.error("用户不存在");

        return ReturnUtil.success("令牌已刷新", Map.of(
                "accessToken", jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getNickname()),
                "refreshToken", jwtUtil.generateRefreshToken(user.getId())
        ));
    }
}
```

- [ ] **Step 2: Build verify**

```bash
mvn clean compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/murasame/controller/AuthController.java
git commit -m "feat: migrate AuthController from session to JWT tokens

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Phase 3: Add JSON API Endpoints for Vue Frontend

The Vue frontend needs JSON data. Instead of modifying existing Thymeleaf controllers (which could break old pages), add a new `ApiController` that returns JSON for each page's data needs.

### Task 3.1: Create ApiController

**Files:** Create `src/main/java/com/murasame/controller/ApiController.java`

- [ ] **Step 1: Write ApiController**

```java
package com.murasame.controller;

import com.murasame.domain.dto.TagWrapper;
import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.domain.vo.CommentVO;
import com.murasame.entity.Blogs;
import com.murasame.entity.Tag;
import com.murasame.entity.Users;
import com.murasame.service.*;
import com.murasame.util.AuthHelper;
import com.murasame.util.BlogHtmlUtil;
import com.murasame.util.ReturnUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestMapping("/api")
@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "API接口", description = "供 Vue 前端使用的 JSON API")
public class ApiController {

    @Resource private IndexService indexService;
    @Resource private BlogService blogService;
    @Resource private CommentService commentService;
    @Resource private TagService tagService;
    @Resource private UserService userService;
    @Resource private LikesService likesService;
    @Resource private AuthHelper authHelper;

    // ===== 博客列表 =====
    @GetMapping("/blogs")
    public Map<String, Object> listBlogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int pageSize) {

        boolean isSearch = (keyword != null && !keyword.isBlank())
                || dateFrom != null || dateTo != null || sortBy != null;

        List<BlogBriefVO> blogBrief;
        long totalBlogs;

        if (isSearch) {
            LocalDateTime from = parseDate(dateFrom, true);
            LocalDateTime to = parseDate(dateTo, false);
            blogBrief = blogService.searchBlogs(keyword, from, to, sortBy, page, pageSize);
            totalBlogs = blogService.countSearchBlogs(keyword, from, to);
        } else {
            blogBrief = indexService.getBlogsByPage(page, pageSize);
            totalBlogs = indexService.getTotalBlogCount();
        }

        int totalPages = (int) Math.ceil((double) totalBlogs / pageSize);

        return ReturnUtil.success(Map.of(
                "blogs", blogBrief,
                "currentPage", page,
                "pageSize", pageSize,
                "totalBlogs", totalBlogs,
                "totalPages", totalPages
        ));
    }

    // ===== 标签列表 =====
    @GetMapping("/tags")
    public Map<String, Object> listTags() {
        List<Tag> tags = tagService.getAllTags();
        return ReturnUtil.success(tags);
    }

    // ===== 热门文章 =====
    @GetMapping("/hot-blogs")
    public Map<String, Object> hotBlogs() {
        List<BlogBriefVO> hot = indexService.getHotBlogs();
        return ReturnUtil.success(hot);
    }

    // ===== 最新评论 =====
    @GetMapping("/recent-comments")
    public Map<String, Object> recentComments() {
        List<CommentVO> comments = commentService.getRecentComments(5);
        return ReturnUtil.success(comments);
    }

    // ===== 博客详情 =====
    @GetMapping("/blogs/{id}")
    public Map<String, Object> blogDetail(@PathVariable Long id, HttpServletRequest request) {
        Blogs blog = blogService.getBlogById(id);
        if (blog == null) return ReturnUtil.error("博客不存在");

        String htmlContent = BlogHtmlUtil.toHtml(blog.getContent());
        blog.setContent(htmlContent);

        List<CommentVO> comments = commentService.getCommentTree(id);
        int commentCount = commentService.getCommentCountByBlogId(id);
        Users authorUser = userService.getUserById(blog.getU_id());
        String authorName = authorUser != null ? authorUser.getNickname() : "未知用户";
        String authorAvatar = authorUser != null ? authorUser.getAvatar() : null;

        // 检查当前用户是否已点赞
        boolean liked = false;
        Users currentUser = authHelper.getCurrentUser(request);
        if (currentUser != null) {
            liked = likesService.isLiked(currentUser.getId(), id);
        }

        return ReturnUtil.success(Map.of(
                "blog", blog,
                "comments", comments,
                "commentCount", commentCount,
                "authorName", authorName,
                "authorAvatar", authorAvatar,
                "liked", liked
        ));
    }

    // ===== 用户主页数据 =====
    @GetMapping("/user/profile/{id}")
    public Map<String, Object> userProfile(@PathVariable Long id) {
        Users user = userService.getUserById(id);
        if (user == null) return ReturnUtil.error("用户不存在");

        int level = userService.calculateLevel(user.getId());
        List<BlogBriefVO> articles = blogService.getBlogBriefsByUserId(user.getId());
        List<Long> likedBlogIds = likesService.getLikedBlogIds(user.getId());
        List<BlogBriefVO> likedBlogs = blogService.getBlogsByIds(likedBlogIds);

        return ReturnUtil.success(Map.of(
                "user", user,
                "level", level,
                "articles", articles != null ? articles : List.of(),
                "likedBlogs", likedBlogs != null ? likedBlogs : List.of()
        ));
    }

    // ===== 归档列表 =====
    @GetMapping("/archives")
    public Map<String, Object> archives(HttpServletRequest request) {
        Users currentUser = authHelper.getCurrentUser(request);
        if (currentUser == null) return ReturnUtil.unauthorized("请先登录");
        List<BlogsBin> bins = blogService.getUserBins(currentUser.getId());
        return ReturnUtil.success(bins);
    }

    private LocalDateTime parseDate(String dateStr, boolean startOfDay) {
        if (dateStr == null || dateStr.isBlank()) return null;
        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return startOfDay ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
    }
}
```

- [ ] **Step 2: Check if BlogService/BlogMapper has the needed methods**

The plan references `getBlogBriefsByUserId`, `getBlogsByIds`, `getUserBins` — these should exist from the recent profile redesign. If any are missing, add them now in the mapper/service.

```bash
grep -n "getBlogBriefsByUserId\|getBlogsByIds\|getUserBins" src/main/java/com/murasame/service/BlogService.java src/main/java/com/murasame/mapper/BlogMapper.java
```

If missing, add them (see Task 3.2).

- [ ] **Step 3: Build verify**

```bash
mvn clean compile
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/murasame/controller/ApiController.java
git commit -m "feat: add ApiController with JSON endpoints for Vue frontend

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 3.2: Add missing mapper/service methods (if needed)

**Files:** Modify `BlogMapper.java`, `BlogService.java`, `BlogServiceImpl.java`

- [ ] **Step 1: Check and add `getBlogBriefsByUserId`**

In `BlogMapper.java`, add if missing:

```java
@Select("SELECT * FROM blogs WHERE u_id = #{userId} ORDER BY created_at DESC")
List<Blogs> getBlogsByUserId(Long userId);
```

In `BlogService.java`:

```java
List<BlogBriefVO> getBlogBriefsByUserId(Long userId);
```

In `BlogServiceImpl.java`:

```java
@Override
public List<BlogBriefVO> getBlogBriefsByUserId(Long userId) {
    List<Blogs> blogs = blogMapper.getBlogsByUserId(userId);
    List<BlogBriefVO> vos = new ArrayList<>();
    for (Blogs blog : blogs) {
        BlogBriefVO vo = new BlogBriefVO();
        vo.setId(blog.getId());
        vo.setU_id(blog.getU_id());
        vo.setTitle(blog.getTitle());
        vo.setBrief(BlogHtmlUtil.extractBrief(blog.getContent()));
        vo.setCreated_at(blog.getCreated_at());
        vo.setUpdated_at(blog.getUpdated_at());
        vo.setAuthor(userService.getNicknameById(blog.getU_id()));
        vo.setT_id(blog.getT_id());
        vo.setRead_count(blog.getRead_count() != null ? blog.getRead_count() : 0L);
        vo.setLike_count(blog.getLike_count() != null ? blog.getLike_count() : 0L);
        vo.setComment_count(commentService.getCommentCountByBlogId(blog.getId()));
        vos.add(vo);
    }
    return vos;
}
```

- [ ] **Step 2: Check and add `getBlogsByIds`**

In `BlogMapper.java`:

```java
@Select("<script>" +
    "SELECT * FROM blogs WHERE id IN " +
    "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
    " ORDER BY created_at DESC" +
    "</script>")
List<Blogs> getBlogsByIds(@Param("ids") List<Long> ids);
```

In `BlogService.java`:

```java
List<BlogBriefVO> getBlogsByIds(List<Long> ids);
```

In `BlogServiceImpl.java`, implement similarly to `getBlogBriefsByUserId` but using `getBlogsByIds`.

- [ ] **Step 3: Check and add `getUserBins`**

In `BlogMapper.java`, if missing:

```java
@Select("SELECT * FROM blogsBin WHERE u_id = #{userId} ORDER BY deleted_at DESC")
List<BlogsBin> getUserBins(Long userId);
```

In `BlogService.java`:

```java
List<BlogsBin> getUserBins(Long userId);
```

In `BlogServiceImpl.java`:

```java
@Override
public List<BlogsBin> getUserBins(Long userId) {
    return blogMapper.getUserBins(userId);
}
```

- [ ] **Step 4: Build verify and commit**

```bash
mvn clean compile
```

```bash
git add src/main/java/com/murasame/mapper/BlogMapper.java \
        src/main/java/com/murasame/service/BlogService.java \
        src/main/java/com/murasame/service/impl/BlogServiceImpl.java
git commit -m "feat: add getBlogBriefsByUserId, getBlogsByIds, getUserBins

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 3.3: Migrate remaining controllers to AuthHelper (dual-run)

**Files:** Modify `BlogController.java`, `ArchiveController.java`, `UserController.java`, `TagController.java`

- [ ] **Step 1: For each controller, apply this mechanical replacement**

For every method that has `HttpSession session` parameter and does `session.getAttribute("currentUser")`:

1. Change parameter type `HttpSession session` → `HttpServletRequest request`
2. Replace `(Users) session.getAttribute("currentUser")` → `authHelper.getCurrentUser(request)`
3. Add `@Resource private AuthHelper authHelper;` to the class
4. Add `import com.murasame.util.AuthHelper;` and `import jakarta.servlet.http.HttpServletRequest;`

The specific controllers to update and their methods:

**BlogController.java** — update `publishBlog`, `editBlog`, `updateBlog`, `deleteBlog`, `recoverBlog`, `likeBlog`, `unlikeBlog`, `isLiked`

**ArchiveController.java** — update all methods with `HttpSession`

**UserController.java** — update all methods with `HttpSession` (comment add, avatar upload, profile update, settings)

**TagController.java** — update if any session usage

- [ ] **Step 2: Run grep to confirm no remaining HttpSession imports in controller constructors**

```bash
grep -rn "HttpSession" src/main/java/com/murasame/controller/ | grep -v "import"
```

Expected: Only AuthController.status(), AuthController.logout(), AuthController.captcha() — which keep HttpSession for backward compatibility (already migrated to dual-run).

- [ ] **Step 3: Build verify**

```bash
mvn clean compile
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/murasame/controller/
git commit -m "feat: migrate controllers from HttpSession to AuthHelper dual-run

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Phase 4: Frontend Project Setup

### Task 4.1: Scaffold Vue 3 + Vite + Element Plus

**Files:** Create `src/main/frontend/package.json`, `vite.config.js`

- [ ] **Step 1: Create src/main/frontend directory**

```bash
mkdir -p src/main/frontend/src/{pages/{index,readBlog,writeBlog,profile,archives,readArchive,followList,error},shared/{components,stores,utils,styles},assets}
```

- [ ] **Step 2: Write package.json**

```json
{
  "name": "myblog-frontend",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.5.13",
    "element-plus": "^2.9.1",
    "@element-plus/icons-vue": "^2.3.1",
    "axios": "^1.7.9",
    "pinia": "^2.3.0",
    "marked": "^15.0.4",
    "dompurify": "^3.2.4"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.2.1",
    "vite": "^6.0.7",
    "sass-embedded": "^1.83.4"
  }
}
```

- [ ] **Step 3: Write vite.config.js**

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import { fileURLToPath } from 'url'

const __dirname = fileURLToPath(new URL('.', import.meta.url))

const pages = ['index', 'readBlog', 'writeBlog', 'profile', 'archives', 'readArchive', 'followList', 'error']

function buildInput() {
  const input = {}
  for (const page of pages) {
    input[page] = resolve(__dirname, `src/pages/${page}/index.html`)
  }
  return input
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': resolve(__dirname, 'src') }
  },
  build: {
    outDir: resolve(__dirname, '../resources/static/dist'),
    emptyOutDir: true,
    rollupOptions: {
      input: buildInput()
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/auth': 'http://localhost:8080',
      '/api': 'http://localhost:8080',
      '/blogs': 'http://localhost:8080',
      '/user': 'http://localhost:8080',
      '/archives': 'http://localhost:8080',
      '/images': 'http://localhost:8080',
      '/pics': 'http://localhost:8080',
      '/css': 'http://localhost:8080',
      '/js': 'http://localhost:8080'
    }
  }
})
```

- [ ] **Step 4: Install dependencies**

```bash
cd src/main/frontend && npm install 2>&1 | tail -5
```

Expected: packages installed successfully.

- [ ] **Step 5: Commit**

```bash
git add src/main/frontend/package.json \
        src/main/frontend/package-lock.json \
        src/main/frontend/vite.config.js
git commit -m "feat: scaffold Vue 3 + Vite + Element Plus frontend

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 4.2: Create shared utilities

**Files:**
- Create: `src/main/frontend/src/utils/token.js`
- Create: `src/main/frontend/src/utils/request.js`
- Create: `src/main/frontend/src/utils/markdown.js`
- Create: `src/main/frontend/src/stores/auth.js`
- Create: `src/main/frontend/src/shared/styles/global.scss`

- [ ] **Step 1: Write token.js**

```javascript
const ACCESS_KEY = 'myblog_access_token'
const REFRESH_KEY = 'myblog_refresh_token'

export function getAccessToken() { return localStorage.getItem(ACCESS_KEY) }
export function getRefreshToken() { return localStorage.getItem(REFRESH_KEY) }
export function setTokens(access, refresh) {
  localStorage.setItem(ACCESS_KEY, access)
  localStorage.setItem(REFRESH_KEY, refresh)
}
export function clearTokens() {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
}
```

- [ ] **Step 2: Write request.js**

```javascript
import axios from 'axios'
import { getAccessToken, getRefreshToken, setTokens, clearTokens } from './token'
import { ElMessage } from 'element-plus'

const request = axios.create({ baseURL: '/', timeout: 15000 })

request.interceptors.request.use(config => {
  const token = getAccessToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

let isRefreshing = false
let refreshSubscribers = []

function onRefreshed(token) {
  refreshSubscribers.forEach(cb => cb(token))
  refreshSubscribers = []
}

request.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config
    if (error.response?.status === 401 && !originalRequest._retry) {
      const refreshToken = getRefreshToken()
      if (!refreshToken) {
        clearTokens()
        return Promise.reject(error)
      }
      if (isRefreshing) {
        return new Promise(resolve => {
          refreshSubscribers.push(token => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            originalRequest._retry = true
            resolve(request(originalRequest))
          })
        })
      }
      isRefreshing = true
      originalRequest._retry = true
      try {
        const { data } = await axios.post('/auth/refresh',
          new URLSearchParams({ refreshToken }),
          { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } })
        if (data.code === 200) {
          setTokens(data.data.accessToken, data.data.refreshToken)
          onRefreshed(data.data.accessToken)
          originalRequest.headers.Authorization = `Bearer ${data.data.accessToken}`
          return request(originalRequest)
        }
      } catch { /* fall through to error */ }
      clearTokens()
      ElMessage.error('登录已过期，请重新登录')
      return Promise.reject(error)
    }
    const msg = error.response?.data?.msg
    if (msg && !originalRequest._silent) ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
```

- [ ] **Step 3: Write markdown.js**

```javascript
import { marked } from 'marked'
import DOMPurify from 'dompurify'

marked.setOptions({ breaks: true, gfm: true })

export function renderMarkdown(md) {
  if (!md) return ''
  return DOMPurify.sanitize(marked.parse(md), {
    ALLOWED_TAGS: ['h1','h2','h3','h4','h5','h6','p','br','hr','ul','ol','li',
      'blockquote','pre','code','strong','em','del','ins','sub','sup',
      'a','img','table','thead','tbody','tr','th','td','div','span'],
    ALLOWED_ATTR: ['href','src','alt','title','class','id','target','rel']
  })
}

export function extractBrief(md, max = 200) {
  if (!md) return ''
  const text = md.replace(/[#*`>\[\]()!_~|]/g, '').replace(/<[^>]*>/g, '').replace(/\s+/g, ' ').trim()
  return text.length > max ? text.substring(0, max) + '...' : text
}
```

- [ ] **Step 4: Write auth.js store**

```javascript
import { defineStore } from 'pinia'
import { getAccessToken, setTokens, clearTokens } from '@/utils/token'
import request from '@/utils/request'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    loggedIn: !!getAccessToken(),
    nickname: '',
    avatar: ''
  }),
  actions: {
    async init() {
      const token = getAccessToken()
      if (!token) { this.loggedIn = false; return }
      try {
        const { data } = await request.get('/auth/status', { _silent: true })
        if (data.code === 200 && data.data?.loggedIn) {
          this.loggedIn = true
          this.nickname = data.data.nickname || ''
          this.avatar = data.data.avatar || ''
        } else { this.logout() }
      } catch { this.loggedIn = false }
    },
    loginSuccess(accessToken, refreshToken, nickname, avatar) {
      setTokens(accessToken, refreshToken)
      this.loggedIn = true
      this.nickname = nickname || ''
      this.avatar = avatar || ''
    },
    async logout() {
      try { await request.post('/auth/logout', {}, { _silent: true }) } catch {}
      clearTokens()
      this.loggedIn = false
      this.nickname = ''
      this.avatar = ''
    }
  }
})
```

- [ ] **Step 5: Write global.scss**

```scss
:root {
  --el-color-primary: #5BA4CF;
  --el-color-primary-light-3: #87CEEB;
  --el-border-radius-base: 8px;
}

* { box-sizing: border-box; }

body {
  margin: 0;
  background-color: #f5f7fa;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

a { text-decoration: none; color: var(--el-color-primary); }

.page-container { max-width: 1200px; margin: 0 auto; padding: 20px 16px; }

.blog-card { cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; }
.blog-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.1); }

.tag-badge {
  display: inline-block; padding: 4px 12px; margin: 3px;
  border-radius: 12px; background: #e8f4fd; color: #5BA4CF;
  font-size: 0.85rem; transition: all 0.2s; cursor: pointer;
  &:hover, &.active { background: #5BA4CF; color: #fff; }
}

.author-avatar-mini { width: 22px; height: 22px; object-fit: cover; border-radius: 50%; margin-right: 4px; vertical-align: middle; }

.blog-content {
  line-height: 1.8; color: #333;
  h1,h2,h3,h4,h5,h6 { margin: 1.2em 0 0.6em; }
  p { margin: 0.8em 0; }
  img { max-width: 100%; border-radius: 4px; }
  pre { background: #f4f4f4; padding: 12px 16px; border-radius: 6px; overflow-x: auto; }
  code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; font-size: 0.9em; }
  blockquote { border-left: 4px solid #5BA4CF; padding-left: 16px; margin: 12px 0; color: #666; }
  table { border-collapse: collapse; width: 100%; margin: 12px 0; }
  th,td { border: 1px solid #e0e0e0; padding: 8px 12px; }
  th { background: #f8f9fa; }
}
```

- [ ] **Step 6: Commit**

```bash
git add src/main/frontend/src/utils/ \
        src/main/frontend/src/stores/ \
        src/main/frontend/src/shared/styles/
git commit -m "feat: add shared frontend utils, auth store, global styles

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Phase 5: Shared Vue Components

### Task 5.1: AppHeader

**Files:** Create `src/main/frontend/src/shared/components/AppHeader.vue`

- [ ] **Step 1: Write AppHeader.vue**

```vue
<template>
  <el-menu mode="horizontal" :default-active="currentPage" class="navbar-custom" @select="handleSelect">
    <div class="header-container">
      <el-menu-item index="index" class="logo-item">
        <span class="logo-text">MyBlog</span>
      </el-menu-item>

      <div class="avatar-area">
        <el-dropdown v-if="auth.loggedIn" trigger="click" @command="handleAvatarCommand">
          <el-avatar :size="40" :src="auth.avatar || '/images/default-avatar.png'" class="header-avatar" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile"><el-icon><User /></el-icon> 个人主页</el-dropdown-item>
              <el-dropdown-item command="editProfile"><el-icon><Edit /></el-icon> 编辑资料</el-dropdown-item>
              <el-dropdown-item command="archives"><el-icon><FolderOpened /></el-icon> 我的归档</el-dropdown-item>
              <el-dropdown-item command="logout" divided><el-icon><SwitchButton /></el-icon> 退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-avatar v-else :size="40" src="/images/default-avatar.png" class="header-avatar" style="cursor:pointer" @click="showAuth = true" />
      </div>

      <div class="header-right">
        <el-menu-item index="index">首页</el-menu-item>
        <el-button type="primary" round class="write-btn" @click="goWrite"><el-icon><Edit /></el-icon> 写作</el-button>
      </div>
    </div>
  </el-menu>

  <AuthModal v-model:visible="showAuth" @login-success="onLogin" />
  <EditProfileModal v-if="auth.loggedIn && showEdit" v-model:visible="showEdit" @saved="onSaved" />
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { User, Edit, FolderOpened, SwitchButton } from '@element-plus/icons-vue'
import AuthModal from './AuthModal.vue'
import EditProfileModal from './EditProfileModal.vue'

defineProps({ currentPage: { type: String, default: 'index' } })

const auth = useAuthStore()
const showAuth = ref(false)
const showEdit = ref(false)

function handleSelect(index) { if (index === 'index') window.location.href = '/' }
function goWrite() { window.location.href = '/blogs/write' }
function handleAvatarCommand(cmd) {
  if (cmd === 'profile') window.location.href = '/user/profile'
  else if (cmd === 'editProfile') showEdit.value = true
  else if (cmd === 'archives') window.location.href = '/archives'
  else if (cmd === 'logout') auth.logout().then(() => { ElMessage.success('已退出'); window.location.reload() })
}
function onLogin() { showAuth.value = false; window.location.reload() }
function onSaved() { showEdit.value = false; window.location.reload() }
</script>

<style scoped>
.header-container { display: flex; align-items: center; width: 100%; max-width: 1200px; margin: 0 auto; padding: 0 16px; }
.logo-item { border-bottom: none !important; }
.logo-text { font-weight: bold; font-size: 1.5rem; color: #fff; }
.avatar-area { flex: 1; display: flex; justify-content: center; }
.header-avatar { cursor: pointer; border: 2px solid rgba(255,255,255,0.5); }
.header-avatar:hover { border-color: #fff; }
.header-right { display: flex; align-items: center; gap: 8px; }
.write-btn { margin-left: 8px; }
.navbar-custom { background-color: rgba(135,206,235,0.8) !important; backdrop-filter: blur(10px); }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/frontend/src/shared/components/AppHeader.vue
git commit -m "feat: add AppHeader component with auth state dropdown

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 5.2: AuthModal

**Files:** Create `src/main/frontend/src/shared/components/AuthModal.vue`

- [ ] **Step 1: Write AuthModal.vue**

```vue
<template>
  <el-dialog :model-value="visible" @update:model-value="$emit('update:visible',$event)" width="440px" :close-on-click-modal="false" center>
    <template #header><div class="auth-dialog-header"><span class="auth-logo">My<span class="auth-logo-hl">Blog</span></span></div></template>

    <el-tabs v-model="activeTab">
      <!-- 登录 -->
      <el-tab-pane label="登录" name="login">
        <el-form ref="loginFormRef" :model="lf" :rules="loginRules" label-position="top" @submit.prevent>
          <el-form-item label="邮箱" prop="email"><el-input v-model="lf.email" placeholder="请输入邮箱" maxlength="255" /></el-form-item>
          <el-form-item label="密码" prop="password"><el-input v-model="lf.password" type="password" placeholder="请输入密码" maxlength="255" show-password /></el-form-item>
          <el-form-item label="图形验证码" prop="captchaCode">
            <div class="captcha-row">
              <el-input v-model="lf.captchaCode" placeholder="不区分大小写" maxlength="4" />
              <img :src="captchaUrl" class="captcha-img" @click="refreshCaptcha" title="点击刷新" />
              <a class="captcha-refresh" @click="refreshCaptcha">换一张</a>
            </div>
          </el-form-item>
          <el-button type="primary" class="auth-submit-btn" @click="handleLogin" :loading="loginLoading">登 录</el-button>
        </el-form>
        <div class="auth-switch"><a @click="activeTab='reset'">忘记密码？</a> 还没有账号？<a @click="activeTab='register'">立即注册</a></div>
      </el-tab-pane>

      <!-- 注册 -->
      <el-tab-pane label="注册" name="register">
        <el-form ref="regFormRef" :model="rf" :rules="regRules" label-position="top" @submit.prevent>
          <el-form-item label="昵称" prop="nickname"><el-input v-model="rf.nickname" placeholder="如何称呼你？" maxlength="32" /></el-form-item>
          <el-form-item label="邮箱" prop="email"><el-input v-model="rf.email" placeholder="请输入邮箱" maxlength="255" /></el-form-item>
          <el-form-item label="密码" prop="password"><el-input v-model="rf.password" type="password" placeholder="至少 6 位密码" maxlength="255" show-password /></el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="rf.confirmPassword" type="password" placeholder="再次输入密码" maxlength="255" show-password /></el-form-item>
          <el-form-item label="邮箱验证码" prop="emailCode">
            <div class="captcha-row">
              <el-input v-model="rf.emailCode" placeholder="6位数字验证码" maxlength="6" />
              <el-button :disabled="sendCd > 0" @click="sendRegCode" size="small">{{ sendCd > 0 ? sendCd+'s' : '发送验证码' }}</el-button>
            </div>
          </el-form-item>
          <el-button type="primary" class="auth-submit-btn" @click="handleRegister" :loading="regLoading">注 册</el-button>
        </el-form>
        <div class="auth-switch">已有账号？<a @click="activeTab='login'">去登录</a></div>
      </el-tab-pane>

      <!-- 重置密码 -->
      <el-tab-pane label="忘记密码" name="reset">
        <el-form ref="resetFormRef" :model="zf" :rules="resetRules" label-position="top" @submit.prevent>
          <el-form-item label="邮箱" prop="email"><el-input v-model="zf.email" placeholder="请输入注册邮箱" maxlength="255" /></el-form-item>
          <el-form-item label="邮箱验证码" prop="emailCode">
            <div class="captcha-row">
              <el-input v-model="zf.emailCode" placeholder="6位数字验证码" maxlength="6" />
              <el-button :disabled="resetCd > 0" @click="sendResetCode" size="small">{{ resetCd > 0 ? resetCd+'s' : '发送验证码' }}</el-button>
            </div>
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword"><el-input v-model="zf.newPassword" type="password" placeholder="至少 6 位新密码" maxlength="255" show-password /></el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword"><el-input v-model="zf.confirmPassword" type="password" placeholder="再次输入新密码" maxlength="255" show-password /></el-form-item>
          <el-button type="primary" class="auth-submit-btn" @click="handleReset" :loading="resetLoading">重置密码</el-button>
        </el-form>
        <div class="auth-switch"><a @click="activeTab='login'">返回登录</a></div>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'

defineProps({ visible: Boolean })
const emit = defineEmits(['update:visible', 'login-success'])
const auth = useAuthStore()
const activeTab = ref('login')

// ===== captcha =====
const captchaToken = ref('')
const captchaUrl = ref('')

async function refreshCaptcha() {
  try {
    const resp = await fetch('/auth/captcha')
    captchaToken.value = resp.headers.get('X-Captcha-Token') || ''
    const blob = await resp.blob()
    captchaUrl.value = URL.createObjectURL(blob)
  } catch {}
}
onMounted(() => refreshCaptcha())

// ===== login =====
const loginFormRef = ref(null), loginLoading = ref(false)
const lf = reactive({ email: '', password: '', captchaCode: '' })
const loginRules = {
  email: [{ required: true, message: '请输入邮箱' }, { type: 'email', message: '格式不正确' }],
  password: [{ required: true, message: '请输入密码' }],
  captchaCode: [{ required: true, message: '请输入验证码' }]
}

async function handleLogin() {
  const valid = await loginFormRef.value?.validate().catch(() => false); if (!valid) return
  loginLoading.value = true
  try {
    const p = new URLSearchParams()
    p.append('email', lf.email); p.append('password', lf.password)
    p.append('captchaCode', lf.captchaCode); p.append('captchaToken', captchaToken.value)
    const { data } = await request.post('/auth/login', p)
    if (data.code === 200) {
      auth.loginSuccess(data.data.accessToken, data.data.refreshToken, data.data.nickname, data.data.avatar)
      ElMessage.success('登录成功'); emit('login-success')
    } else refreshCaptcha()
  } finally { loginLoading.value = false }
}

// ===== register =====
const regFormRef = ref(null), regLoading = ref(false), sendCd = ref(0)
const rf = reactive({ nickname: '', email: '', password: '', confirmPassword: '', emailCode: '' })
const regRules = {
  nickname: [{ required: true, message: '请输入昵称' }],
  email: [{ required: true, message: '请输入邮箱' }, { type: 'email', message: '格式不正确' }],
  password: [{ required: true, min: 6, message: '密码至少6位' }],
  confirmPassword: [{ required: true, message: '请确认密码' }, { validator: (r, v, cb) => cb(v !== rf.password ? new Error('两次输入不一致') : undefined), trigger: 'blur' }],
  emailCode: [{ required: true, message: '请输入验证码' }]
}

async function sendRegCode() {
  try {
    await request.post('/auth/send-code', new URLSearchParams({ email: rf.email }))
    ElMessage.success('验证码已发送')
    sendCd.value = 60; const t = setInterval(() => { sendCd.value--; if (sendCd.value <= 0) clearInterval(t) }, 1000)
  } catch {}
}

async function handleRegister() {
  const valid = await regFormRef.value?.validate().catch(() => false); if (!valid) return
  regLoading.value = true
  try {
    const p = new URLSearchParams(); p.append('nickname', rf.nickname); p.append('email', rf.email)
    p.append('password', rf.password); p.append('emailCode', rf.emailCode)
    const { data } = await request.post('/auth/register', p)
    if (data.code === 200) {
      auth.loginSuccess(data.data.accessToken, data.data.refreshToken, data.data.nickname, data.data.avatar)
      ElMessage.success('注册成功'); emit('login-success')
    }
  } finally { regLoading.value = false }
}

// ===== reset password =====
const resetFormRef = ref(null), resetLoading = ref(false), resetCd = ref(0)
const zf = reactive({ email: '', emailCode: '', newPassword: '', confirmPassword: '' })
const resetRules = {
  email: [{ required: true, message: '请输入邮箱' }, { type: 'email', message: '格式不正确' }],
  emailCode: [{ required: true, message: '请输入验证码' }],
  newPassword: [{ required: true, min: 6, message: '密码至少6位' }],
  confirmPassword: [{ required: true, message: '请确认密码' }, { validator: (r, v, cb) => cb(v !== zf.newPassword ? new Error('两次输入不一致') : undefined), trigger: 'blur' }]
}

async function sendResetCode() {
  try {
    await request.post('/auth/send-reset-code', new URLSearchParams({ email: zf.email }))
    ElMessage.success('验证码已发送')
    resetCd.value = 60; const t = setInterval(() => { resetCd.value--; if (resetCd.value <= 0) clearInterval(t) }, 1000)
  } catch {}
}

async function handleReset() {
  const valid = await resetFormRef.value?.validate().catch(() => false); if (!valid) return
  resetLoading.value = true
  try {
    const p = new URLSearchParams(); p.append('email', zf.email)
    p.append('newPassword', zf.newPassword); p.append('emailCode', zf.emailCode)
    const { data } = await request.post('/auth/reset-password', p)
    if (data.code === 200) { ElMessage.success('密码重置成功'); activeTab.value = 'login' }
  } finally { resetLoading.value = false }
}
</script>

<style scoped>
.auth-dialog-header { text-align: center; } .auth-logo { font-size: 1.5rem; font-weight: bold; }
.auth-logo-hl { color: var(--el-color-primary); } .auth-submit-btn { width: 100%; margin-top: 12px; height: 44px; font-size: 1rem; }
.auth-switch { text-align: center; margin-top: 16px; font-size: 0.85rem; color: #999; }
.auth-switch a { color: var(--el-color-primary); cursor: pointer; }
.captcha-row { display: flex; gap: 10px; align-items: center; width: 100%; }
.captcha-img { height: 40px; cursor: pointer; border-radius: 6px; border: 1px solid #e0e0e0; }
.captcha-refresh { font-size: 0.78rem; color: var(--el-color-primary); cursor: pointer; white-space: nowrap; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/frontend/src/shared/components/AuthModal.vue
git commit -m "feat: add AuthModal with login/register/reset tabs and captcha

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 5.3: CommentSection + EditProfileModal + WeatherWidget

**Files:** Create three component files.

- [ ] **Step 1: Write CommentSection.vue**

```vue
<template>
  <div class="comment-section">
    <el-divider />
    <h4>评论 ({{ comments.length }})</h4>

    <div v-if="auth.loggedIn" class="comment-input-box">
      <el-input v-model="newComment" type="textarea" :rows="3" placeholder="写下你的评论..." maxlength="500" show-word-limit />
      <el-button type="primary" size="small" class="comment-submit-btn" @click="submitComment" :loading="submitting">发表评论</el-button>
    </div>
    <el-alert v-else title="请先登录后发表评论" type="info" :closable="false" show-icon />

    <el-empty v-if="comments.length === 0" description="暂无评论" />

    <div v-for="c in comments" :key="c.id" class="comment-item">
      <div class="comment-header">
        <img :src="c.author_avatar || '/images/default-avatar.png'" class="comment-avatar" />
        <span class="comment-author">{{ c.author_name }}</span>
        <span class="comment-time">{{ formatTime(c.created_at) }}</span>
      </div>
      <div class="comment-body" v-html="c.content"></div>
      <el-button text size="small" @click="toggleReply(c.id)"><el-icon><ChatLineSquare /></el-icon> 回复</el-button>

      <div v-if="replyTarget === c.id" class="reply-box">
        <el-input v-model="replyContent" type="textarea" :rows="2" placeholder="写下回复..." maxlength="500" show-word-limit />
        <el-button type="primary" size="small" @click="submitReply(c.id)" :loading="replySub">回复</el-button>
        <el-button size="small" @click="replyTarget = null">取消</el-button>
      </div>

      <div v-if="c.children?.length" class="child-comments">
        <div v-for="ch in c.children" :key="ch.id" class="comment-item child">
          <div class="comment-header">
            <img :src="ch.author_avatar || '/images/default-avatar.png'" class="comment-avatar" />
            <span class="comment-author">{{ ch.author_name }}</span>
            <span class="comment-time">{{ formatTime(ch.created_at) }}</span>
          </div>
          <div class="comment-body" v-html="ch.content"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatLineSquare } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'

const props = defineProps({ blogId: { type: Number, required: true }, comments: { type: Array, default: () => [] } })
const emit = defineEmits(['comment-added'])
const auth = useAuthStore()

const newComment = ref(''), submitting = ref(false)
const replyTarget = ref(null), replyContent = ref(''), replySub = ref(false)

function formatTime(d) { return d ? new Date(d).toLocaleString('zh-CN', { year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit' }) : '' }

async function submitComment() {
  if (!newComment.value.trim()) return
  submitting.value = true
  try {
    const { data } = await request.post('/user/comment/add', new URLSearchParams({ bId: props.blogId, content: newComment.value.trim() }))
    if (data.code === 200) { ElMessage.success('评论成功'); newComment.value = ''; emit('comment-added') }
  } finally { submitting.value = false }
}

function toggleReply(id) { replyTarget.value = replyTarget.value === id ? null : id; replyContent.value = '' }

async function submitReply(parentId) {
  if (!replyContent.value.trim()) return
  replySub.value = true
  try {
    const { data } = await request.post('/user/comment/add', new URLSearchParams({ bId: props.blogId, parentId, content: replyContent.value.trim() }))
    if (data.code === 200) { ElMessage.success('回复成功'); replyTarget.value = null; emit('comment-added') }
  } finally { replySub.value = false }
}
</script>

<style scoped>
.comment-section { margin-top: 24px; } .comment-input-box { margin-bottom: 16px; } .comment-submit-btn { margin-top: 8px; }
.comment-item { padding: 16px 0; border-bottom: 1px solid #f0f0f0; } .comment-item.child { margin-left: 40px; }
.comment-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.comment-avatar { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; }
.comment-author { font-weight: 600; } .comment-time { font-size: 0.8rem; color: #999; margin-left: auto; }
.comment-body { color: #555; line-height: 1.6; }
.reply-box { margin: 8px 0; display: flex; gap: 8px; flex-wrap: wrap; }
.child-comments { margin-top: 12px; border-left: 2px solid #e8f4fd; padding-left: 12px; }
</style>
```

- [ ] **Step 2: Write EditProfileModal.vue**

```vue
<template>
  <el-dialog :model-value="visible" @update:model-value="$emit('update:visible',$event)" title="编辑资料" width="480px" :close-on-click-modal="false">
    <el-form label-position="top">
      <el-form-item label="头像">
        <div class="edit-avatar-area">
          <el-avatar :size="80" :src="avatarPreview" />
          <div><input ref="fi" type="file" accept="image/*" style="display:none" @change="onFile"><el-button size="small" @click="$refs.fi.click()">上传新头像</el-button><div v-if="avatarMsg" class="avatar-msg">{{ avatarMsg }}</div></div>
        </div>
      </el-form-item>
      <el-form-item label="昵称"><el-input v-model="form.nickname" maxlength="32" /></el-form-item>
      <el-form-item label="简介"><el-input v-model="form.intro" maxlength="255" /></el-form-item>
      <el-form-item label="邮箱"><el-input v-model="form.email" maxlength="255" /></el-form-item>
      <el-form-item label="性别">
        <el-select v-model="form.gender">
          <el-option label="未设置" :value="0" /><el-option label="男" :value="1" /><el-option label="女" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="GitHub 用户名"><el-input v-model="form.githubUsername" maxlength="255" /></el-form-item>
      <el-form-item>
        <template #label>GitHub Token <span class="subtle-hint">（<a href="https://github.com/settings/tokens" target="_blank">获取方式</a>，需勾选 read:user）</span></template>
        <el-input v-model="form.githubToken" type="password" maxlength="255" placeholder="ghp_xxx" autocomplete="off" />
      </el-form-item>
    </el-form>
    <template #footer><el-button @click="$emit('update:visible',false)">取消</el-button><el-button type="primary" @click="save" :loading="saving">保存</el-button></template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

defineProps({ visible: Boolean })
const emit = defineEmits(['update:visible', 'saved'])
const avatarPreview = ref('/images/default-avatar.png'), avatarMsg = ref(''), saving = ref(false)
const form = reactive({ nickname:'',intro:'',email:'',gender:0,githubUsername:'',githubToken:'' })

onMounted(async () => {
  try {
    const { data } = await request.get('/user/profile/data'); if (data.code === 200) { const u = data.data; Object.assign(form, { nickname:u.nickname||'',intro:u.intro||'',email:u.email||'',gender:u.gender||0,githubUsername:u.githubUsername||'' }); avatarPreview.value = u.avatar || '/images/default-avatar.png' }
  } catch {}
})

async function onFile(e) {
  const file = e.target.files?.[0]; if (!file) return
  if (file.size > 10*1024*1024) { avatarMsg.value = '图片不能超过10MB'; return }
  avatarMsg.value = '上传中...'; const fd = new FormData(); fd.append('file', file)
  try { const { data } = await request.post('/user/avatar/upload', fd); if (data.code === 200) { avatarPreview.value = data.data+'?t='+Date.now(); avatarMsg.value = '头像已更新' } } catch { avatarMsg.value = '上传失败' }
  e.target.value = ''
}

async function save() {
  saving.value = true
  try {
    const p = new URLSearchParams(); p.append('nickname',form.nickname); p.append('intro',form.intro); p.append('email',form.email); p.append('gender',form.gender); p.append('githubUsername',form.githubUsername); if (form.githubToken) p.append('githubToken',form.githubToken)
    const { data } = await request.post('/user/profile/update', p)
    if (data.code === 200) { ElMessage.success('保存成功'); emit('saved') }
  } finally { saving.value = false }
}
</script>

<style scoped>
.edit-avatar-area { display: flex; gap: 16px; align-items: center; } .avatar-msg { font-size: 0.72rem; color: #999; margin-top: 4px; }
.subtle-hint { font-size: 0.68rem; color: #aaa; font-weight: 400; } .subtle-hint a { color: var(--el-color-primary); }
</style>
```

- [ ] **Step 3: Write WeatherWidget.vue**

```vue
<template>
  <el-card class="weather-card">
    <div v-if="loading" class="weather-loading"><img src="/pics/weather/loading.svg" width="40" alt="loading" /></div>
    <div v-else-if="error" class="weather-error">天气不可用</div>
    <div v-else class="weather-content">
      <img :src="iconSrc" :alt="weather.text" class="weather-icon" />
      <span class="weather-temp">{{ weather.temperature }}°C</span>
      <span class="weather-text">{{ weather.text }}</span>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
const weather = ref({ temperature: '--', text: '加载中' })
const loading = ref(true), error = ref(false)
const iconSrc = ref('/pics/weather/loading.svg')

const iconMap = {
  '晴': 'sunny', '多云': 'cloudy', '阴': 'cloudy',
  '雨': 'rainy', '雪': 'snowy', '雾': 'foggy'
}

onMounted(async () => {
  try {
    const resp = await fetch('/api/weather/now?location=beijing')
    const data = await resp.json()
    if (data.code === 200 && data.data) {
      weather.value = data.data
      const w = data.data.text || ''
      let icon = 'unknown'
      for (const [k, v] of Object.entries(iconMap)) { if (w.includes(k)) { icon = v; break } }
      iconSrc.value = `/pics/weather/${icon}.svg`
    } else error.value = true
  } catch { error.value = true }
  loading.value = false
})
</script>

<style scoped>
.weather-card { text-align: center; } .weather-content { display: flex; align-items: center; gap: 8px; justify-content: center; }
.weather-icon { width: 40px; height: 40px; } .weather-temp { font-size: 1.5rem; font-weight: bold; } .weather-text { color: #666; }
</style>
```

- [ ] **Step 4: Commit**

```bash
git add src/main/frontend/src/shared/components/CommentSection.vue \
        src/main/frontend/src/shared/components/EditProfileModal.vue \
        src/main/frontend/src/shared/components/WeatherWidget.vue
git commit -m "feat: add CommentSection, EditProfileModal, WeatherWidget components

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Phase 6: Page-by-Page Migration

Each page follows the same pattern:
1. Create `src/main/frontend/src/pages/<name>/index.html` (thin HTML shell)
2. Create `src/main/frontend/src/pages/<name>/main.js` (Vue mount)
3. Create `src/main/frontend/src/pages/<name>/<PageName>.vue` (page component)
4. Update corresponding Thymeleaf template to serve the Vue shell

### Task 6.1: Index page (blog list)

**Files:**
- Create: `src/main/frontend/src/pages/index/index.html`
- Create: `src/main/frontend/src/pages/index/main.js`
- Create: `src/main/frontend/src/pages/index/IndexPage.vue`
- Modify: `src/main/resources/templates/index.html`

**index.html:**
```html
<!DOCTYPE html>
<html lang="zh">
<head><meta charset="UTF-8"><title>博客主页</title></head>
<body><div id="app"></div><script type="module" src="./main.js"></script></body>
</html>
```

**main.js:**
```javascript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import 'element-plus/dist/index.css'
import '@/shared/styles/global.scss'
import IndexPage from './IndexPage.vue'

const app = createApp(IndexPage)
app.use(createPinia())
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
```

**IndexPage.vue** (~200 lines): Uses `el-row`/`el-col` layout. Left sidebar: WeatherWidget + tags (`el-card`). Center: search bar (`el-input` + `el-button`), filter panel (`el-radio-group`), blog cards (`el-card` with `el-skeleton` loading), `el-pagination`. Right sidebar: hot blogs + recent comments (`el-card`). Calls `/api/blogs`, `/api/tags`, `/api/hot-blogs`, `/api/recent-comments`.

**Template update:** Replace `src/main/resources/templates/index.html` content with a thin shell that loads the Vue build output (see Phase 8).

### Task 6.2: readBlog page

**Files:** Create `index.html`, `main.js`, `ReadBlogPage.vue` in `src/main/frontend/src/pages/readBlog/`

**ReadBlogPage.vue** (~180 lines): AppHeader + blog content (`v-html` with DOMPurify-sanitized markdown), meta bar (author, date, reads, likes), like/unlike buttons (check `el-button` with icon, toggling liked state), CommentSection. Calls `/api/blogs/{id}`, `/blogs/like/{id}`, `/blogs/unlike/{id}`, `/blogs/incrementRead/{id}`.

### Task 6.3: writeBlog page

**Files:** Create in `src/main/frontend/src/pages/writeBlog/`

**WriteBlogPage.vue** (~150 lines): `el-form` with title (`el-input`), content (`el-input type=textarea` or Markdown editor), tag selection (`el-select multiple` with existing tags + `el-input` for new tags), publish/submit (`el-button`). On edit mode, pre-fill from blog data. Calls `/blogs/publish` or `/blogs/update`, `/api/tags`.

### Task 6.4: profile page

**Files:** Create in `src/main/frontend/src/pages/profile/`

**ProfilePage.vue** (~120 lines): `el-card` with user info (avatar, nickname, level), `el-tabs` (articles tab, liked tab). Calls `/api/user/profile/{id}`.

### Task 6.5: archives page

**Files:** Create in `src/main/frontend/src/pages/archives/`

**ArchivesPage.vue** (~80 lines): List of deleted blogs with recover buttons. Calls `/api/archives`, `/blogs/recover/{id}`.

### Task 6.6: followList + error pages

**Files:** Create in respective `src/main/frontend/src/pages/` directories.

**FollowListPage.vue** (~60 lines): List of followed users.
**ErrorPage.vue** (~30 lines): `el-result` with error message.

---

## Phase 7: Build & Serve Integration

### Task 7.1: Build frontend and verify output

- [ ] **Step 1: Build frontend**

```bash
cd src/main/frontend && npm run build 2>&1 | tail -10
```

Expected: Build succeeds, files written to `src/main/resources/static/dist/`.

- [ ] **Step 2: Verify output files**

```bash
ls -la src/main/resources/static/dist/
```

Expected: `assets/`, `index.html`, `readBlog.html`, etc.

- [ ] **Step 3: Register dist resource handler in WebConfiguration**

Read `WebConfiguration.java`. If `/dist/**` is not already mapped, add:

```java
registry.addResourceHandler("/dist/**")
        .addResourceLocations("classpath:/static/dist/");
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/dist/ src/main/java/com/murasame/config/WebConfiguration.java
git commit -m "feat: add Vue build output and dist resource handler

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Phase 8: Update Thymeleaf Templates to Serve Vue Shells

### Task 8.1: Rewrite each Thymeleaf template as a thin shell

For each Thymeleaf template in `src/main/resources/templates/`, replace the full content with a minimal HTML shell that loads the Vue app.

**Pattern (for index.html):**
```html
<!DOCTYPE html>
<html lang="zh">
<head>
  <meta charset="UTF-8">
  <title>博客主页</title>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css">
</head>
<body>
  <div id="app"></div>
  <link rel="stylesheet" href="/dist/assets/index-[hash].css">
  <script type="module" src="/dist/assets/index-[hash].js"></script>
</body>
</html>
```

**IMPORTANT:** After `npm run build`, the generated JS/CSS files have hashed names. Read the `dist/` directory to get exact filenames before writing template links.

Alternatively, Vite generates an `index.html` per entry — copy those to the Thymeleaf template locations and wrap in `th:replace` fragments as needed.

**Files to replace:**
- `templates/index.html` → loads `/dist/assets/index-*.js`
- `templates/readBlog.html` → loads `/dist/assets/readBlog-*.js`
- `templates/writeBlog.html` → loads `/dist/assets/writeBlog-*.js`
- `templates/profile.html` → loads `/dist/assets/profile-*.js`
- `templates/archives.html` → loads `/dist/assets/archives-*.js`
- `templates/readArchive.html` → loads `/dist/assets/readArchive-*.js`
- `templates/follow-list.html` → loads `/dist/assets/followList-*.js`
- `templates/error.html` → loads `/dist/assets/error-*.js`

Since Vite output has hashed filenames, a better approach: **copy the built `index.html` from each page's output from `dist/` to the templates directory.** Vite already generates correct `<script>` and `<link>` tags with hashed names.

- [ ] **Step 1: After each build, copy dist outputs to templates**

For each page, copy the built HTML as the Thymeleaf template:

```bash
cp src/main/resources/static/dist/index.html src/main/resources/templates/index.html
cp src/main/resources/static/dist/readBlog.html src/main/resources/templates/readBlog.html
# ... etc
```

Then adjust each template to reference assets correctly (Vite builds use relative paths; we need absolute paths from `/dist/`).

- [ ] **Step 2: Start app and test**

```bash
mvn spring-boot:run
```

Open browser, navigate to `/` and verify the Vue app loads.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/templates/
git commit -m "feat: replace Thymeleaf templates with Vue build shells

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Phase 9: Cleanup

### Task 9.1: Remove deprecated files

- [ ] **Step 1: Delete old frontend assets**

```bash
git rm src/main/resources/static/js/*.js
git rm src/main/resources/static/css/*.css
```

(Keep `images/`, `pics/`, `ReadMePics/` — still referenced by Vue components and README.)

- [ ] **Step 2: Delete UserInterceptor**

```bash
git rm src/main/java/com/murasame/interceptor/UserInterceptor.java
```

- [ ] **Step 3: Remove UserInterceptor from WebConfiguration**

Remove the `@Autowired private UserInterceptor userInterceptor;` field and the interceptor registration block from `WebConfiguration.java`.

- [ ] **Step 4: Remove Thymeleaf fragments**

```bash
git rm src/main/resources/templates/fragment/header.html
git rm src/main/resources/templates/fragment/weather.html
```

- [ ] **Step 5: Remove any unused dependencies from pom.xml**

Remove `spring-boot-starter-thymeleaf` if it was the only Thymeleaf dependency (check if it's still needed for Spring MVC views — it is needed until ALL templates are removed. Keep it for now since error.html and other templates may still reference it).

- [ ] **Step 6: Build and run full test suite**

```bash
mvn clean test
```

Expected: All tests pass.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "chore: remove deprecated frontend assets, UserInterceptor, old fragments

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Verification Checklist

After each phase, run a manual smoke test:

1. Start app: `mvn spring-boot:run`
2. Open `http://localhost:8080/` — blog list loads (Vue or Thymeleaf depending on phase)
3. Click a blog card → blog detail loads
4. Click "写作" — write form loads
5. Open auth modal → captcha loads → login works → header shows avatar
6. Write a test article → publish → appears in list
7. Read the article → comment → comment appears
8. Like the article → like count increments
9. Close browser tab → reopen → still logged in (JWT persisted)
10. Run tests: `mvn test` — all pass

---

## Estimated Implementation Time

| Phase | Tasks | Estimate |
|-------|-------|----------|
| 1: JWT Infrastructure | 1.1–1.6 | 2–3 hours |
| 2: Auth Migration | 2.1 | 1 hour |
| 3: API Endpoints | 3.1–3.3 | 2–3 hours |
| 4: Frontend Setup | 4.1–4.2 | 1 hour |
| 5: Shared Components | 5.1–5.3 | 3–4 hours |
| 6: Pages | 6.1–6.6 | 5–7 hours |
| 7: Build & Serve | 7.1 | 30 min |
| 8: Template Shells | 8.1 | 30 min |
| 9: Cleanup | 9.1 | 30 min |
| **Total** | | **16–21 hours** |
