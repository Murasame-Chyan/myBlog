# Vue3 + Element Plus + JWT Auth Migration Design

## Overview

Migrate the blog from server-rendered Thymeleaf + session auth to Vue 3 MPA + JWT token auth. Incremental migration: one page at a time, dual-run session+JWT during transition.

## Architecture

```
Browser (Vue 3 MPA — each page is an independent Vue app)
  ├── Axios interceptor: attaches Authorization: Bearer <jwt> to all requests
  ├── JWT stored in localStorage (access + refresh tokens)
  └── Element Plus components replacing Bootstrap 5

Spring Boot (REST API + Thymeleaf page shells)
  ├── JwtAuthenticationFilter — parses JWT, injects SecurityContext
  ├── UserInterceptor — kept during transition for session compatibility
  ├── Controllers — prefer SecurityContext, fallback to session
  └── Static resource serving — Vue build output under /static/dist/
```

## JWT Design

- Access Token: 30 min TTL, contains userId, email, nickname
- Refresh Token: 7 day TTL, contains userId + type=refresh
- Storage: localStorage (both tokens)
- Auto-refresh: Axios response interceptor catches 401, calls /auth/refresh
- Revocation: Redis blacklist key `jwt:blacklist:<userId>` with timestamp; tokens issued before the timestamp are rejected
- Secret: `${JWT_SECRET:}` placeholder in application.yml, actual value in external application.properties

## Backend Changes

### New files
- `JwtUtil.java` — issue, validate, parse JWT
- `JwtAuthenticationFilter.java` — OncePerRequestFilter, replaces UserInterceptor
- `JwtProperties.java` — @ConfigurationProperties for jwt.* config

### Modified files
- `SecurityConfiguration.java` — register JwtFilter, add /auth/refresh endpoint
- `AuthController.java` — login/register return JWT tokens; add /auth/refresh; captcha keyed by captchaToken instead of sessionId
- `BlogController.java` — HttpSession → JWT claims
- `ArchiveController.java` — same
- `UserController.java` — same
- `TagController.java` — same
- `CommentController.java` — same
- `application.yml` — add jwt.* placeholder config

### Removed (after full migration)
- `UserInterceptor.java`

## Frontend Changes

### Tech stack
- Vue 3 + Composition API (JavaScript, not TypeScript)
- Vite multi-entry build
- Element Plus component library
- Axios HTTP client
- @element-plus/icons-vue
- marked + DOMPurify (Markdown rendering + XSS sanitization)
- Pinia (login state store)

### Project structure
```
src/main/frontend/
├── vite.config.js
├── package.json
├── src/
│   ├── pages/
│   │   ├── index/          — Blog list page
│   │   ├── readBlog/       — Blog detail page
│   │   ├── writeBlog/      — Write/edit page
│   │   ├── profile/        — User profile
│   │   ├── archives/       — Archive list
│   │   ├── readArchive/    — Archive detail
│   │   └── followList/     — Follow list
│   ├── shared/
│   │   ├── components/     — Header, Footer, Comment, AuthModal
│   │   ├── stores/         — authStore (Pinia)
│   │   ├── utils/          — axios instance, token helpers, markdown
│   │   └── styles/         — Global style variables
│   └── App.vue
```

Build output → `src/main/resources/static/dist/`

### Major Element Plus component usage
| Page | Components |
|------|-----------|
| index | el-card, el-pagination, el-input, el-select, el-tag, el-skeleton |
| readBlog | el-card, el-button, el-divider, el-input (comments), el-skeleton |
| writeBlog | el-form, el-input, el-select, el-button, el-upload, el-dialog |
| profile | el-card, el-tabs, el-descriptions, el-skeleton |
| header | el-menu, el-dropdown, el-dialog (auth modal), el-avatar |
| global | el-message, el-empty, el-alert |

## Incremental Migration Order

1. **Backend JWT** — dual-run: JwtFilter + UserInterceptor coexist
2. **index page** — blog list, pagination, search, filters, tags, hot blogs
3. **header component** — auth state UI, login/register/reset modals
4. **readBlog page** — blog detail, comments, likes
5. **writeBlog page** — create/edit form, tag selection, image upload
6. **profile / archives / followList** — remaining pages
7. **Cleanup** — remove Thymeleaf templates, old JS/CSS, UserInterceptor, unused dependencies

## Security Checklist

| # | Rule | Migration impact |
|---|------|-----------------|
| 1 | Ownership checks | Controller logic unchanged |
| 2 | No mass-destruction endpoints | Unchanged |
| 3 | Auth on every endpoint | JwtFilter unified handling |
| 4 | Sanitize before v-html/utext | Use DOMPurify client-side for blog content |
| 5 | File upload magic bytes | Backend validation unchanged |
| 6 | SSL bypass opt-in | Unchanged |
| 7 | Email uniqueness | Unchanged |
| 8 | Confirm for destructive actions | el-popconfirm / ElMessageBox.confirm |

## Testing

- Create `src/test` with JUnit 5 + Mockito
- Unit tests for JwtUtil (generate, validate, expire, blacklist)
- Unit tests for JwtAuthenticationFilter (valid token, expired token, missing header)
- Integration tests for AuthController (login returns tokens, refresh flow)
- Manual smoke test checklist after each page migration

## Weather & GitHub Widgets

- Weather API endpoints unchanged; wrapped in a Vue component that calls existing `/api/weather/*`
- GitHub heatmap kept as server-rendered fragment if JS logic is too complex to port; otherwise wrapped in a Vue component
- If porting proves impractical, leave as-is inside the Vue page shell

## Config

```yaml
# application.yml (classpath — placeholders only)
jwt:
  secret: ${JWT_SECRET:}
  access-token-expiration: ${JWT_ACCESS_EXPIRATION:1800000}
  refresh-token-expiration: ${JWT_REFRESH_EXPIRATION:604800000}
```

```properties
# External application.properties (next to jar — real values)
jwt.secret=<openssl rand -base64 32>
```
