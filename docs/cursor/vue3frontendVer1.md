# Vue3 前端迁移 Ver.1 — 后端对接说明

> 供后端 Agent 查阅。前端已按 `docs/superpowers/plans/2026-05-20-vue3-frontend-implementation.md` 完成实现；**当前仓库 Java 侧尚无 JWT 版 `/api/*` 与 `ApiController`**，需对照 `docs/superpowers/plans/2026-05-20-vue3-jwt-migration.md` 补齐后端后联调。

---

## 1. 架构概览

| 项 | 说明 |
|----|------|
| 模式 | MPA（多页应用），**无 Vue Router**，页面跳转用 `window.location.href` |
| 技术栈 | Vue 3.5 + Vite 6 + Element Plus 2.9 + Pinia + Axios + marked + DOMPurify |
| 源码目录 | `src/main/frontend/` |
| 构建输出 | `src/main/resources/static/dist/`（`npm run build`） |
| 页面壳 | `src/main/resources/templates/*.html` 已替换为极简 HTML，加载 `/dist/assets/<page>-<hash>.js` |
| 鉴权 | JWT：`Authorization: Bearer <accessToken>`；401 时前端用 `refreshToken` 调 `/auth/refresh` |

---

## 2. 前端已实现的页面与路由

| 浏览器路由 | Thymeleaf 模板 | Vue 入口 | 主要 API |
|-----------|----------------|----------|----------|
| `/`、`/index` | `templates/index.html` | `IndexPage.vue` | `/api/blogs`, `/api/tags`, `/api/hot-blogs`, `/api/recent-comments` |
| `/blogs/read/{id}` | `templates/readBlog.html` | `ReadBlogPage.vue` | `/api/blogs/{id}`, `/blogs/like|unlike|incrementRead/{id}`, `/user/comment/add` |
| `/blogs/write` | `templates/writeBlog.html` | `WriteBlogPage.vue` | `/api/tags`, `/blogs/publish` |
| `/blogs/edit/{id}` | 同上（路径解析） | `WriteBlogPage.vue` | `/api/blogs/{id}`, `/blogs/update` |
| `/user/profile?id={id}` | `templates/profile.html` | `ProfilePage.vue` | `/api/user/profile/{id}` |
| `/archives` | `templates/archives.html` | `ArchivesPage.vue` | `/api/archives`, `/blogs/recover/{id}` |
| `/archives/read/{id}` | `templates/readArchive.html` | `ReadArchivePage.vue` | `/api/archives/{id}`（优先）或 `/api/archives` 列表筛选 |
| `/user/{id}/followers` | `templates/follow-list.html` | `FollowListPage.vue` | `/user/follow/followers?userId=&page=&pageSize=` |
| `/user/{id}/following` | 同上 | `FollowListPage.vue` | `/user/follow/following?userId=&page=&pageSize=` |
| `/error` 等 | `templates/error.html` | `ErrorPage.vue` | 无（可选 `?msg=` 查询参数） |

**仍走服务端路由（未改 Java）：** `/blogs/tag/{id}` 标签筛选、`/user/follow/{id}`、`/user/unfollow/{id}` 等。

---

## 3. 统一响应格式（前端约定）

所有 JSON 接口期望：

```json
{ "code": 200, "msg": "...", "data": ... }
```

失败示例：

```json
{ "code": 401, "msg": "请先登录" }
```

前端 `src/main/frontend/src/utils/request.js` 在 `code !== 200` 或 HTTP 错误时会用 `ElMessage.error(msg)`（除非请求带 `_silent: true`）。

---

## 4. 认证 API（`/auth/*`）

| 端点 | 方法 | Content-Type | 参数 | 成功 `data` 字段 |
|------|------|--------------|------|------------------|
| `/auth/login` | POST | `application/x-www-form-urlencoded` | `email`, `password`, `captchaCode`, `captchaToken` | `accessToken`, `refreshToken`, `expiresIn`, `nickname`, `avatar` |
| `/auth/register` | POST | 同上 | `email`, `nickname`, `password`, `emailCode` | `accessToken`, `refreshToken`, `nickname`, `avatar` |
| `/auth/captcha` | GET | — | — | 图片 body；**响应头** `X-Captcha-Token` 为令牌 |
| `/auth/send-code` | POST | urlencoded | `email` | — |
| `/auth/send-reset-code` | POST | urlencoded | `email` | — |
| `/auth/reset-password` | POST | urlencoded | `email`, `newPassword`, `emailCode` | — |
| `/auth/refresh` | POST | urlencoded | `refreshToken` | `accessToken`, `refreshToken` |
| `/auth/logout` | POST | — | 需 Bearer | — |
| `/auth/status` | GET | — | 需 Bearer | `loggedIn`, `nickname`, `avatar`（可选） |

**localStorage 键名（固定）：**

- `myblog_access_token`
- `myblog_refresh_token`

---

## 5. 数据 API（`/api/*`）— 后端需新增或对齐

### 5.1 博客列表 `GET /api/blogs`

**Query：** `keyword?`, `dateFrom?`（`yyyy-MM-dd`）, `dateTo?`, `sortBy?`（`newest|oldest|likes|reads|comments`）, `page`（默认 1）, `pageSize`（前端首页用 **5**）

**`data` 结构：**

```json
{
  "blogs": [ /* BlogBriefVO[] */ ],
  "currentPage": 1,
  "pageSize": 5,
  "totalBlogs": 100,
  "totalPages": 20
}
```

**`BlogBriefVO` 字段（snake_case，与现有 VO 一致）：**

`id`, `u_id`, `title`, `brief`, `created_at`, `updated_at`, `author`, `author_avatar`, `t_id`（`{ tagList: [1,2,3] }`）, `read_count`, `like_count`, `comment_count`

### 5.2 标签 `GET /api/tags`

**`data`：** `[{ "id": 1, "tagName": "Java" }, ...]`

### 5.3 热门文章 `GET /api/hot-blogs`

**`data`：** `BlogBriefVO[]`

### 5.4 最新评论 `GET /api/recent-comments`

**`data`：** `CommentVO[]`（字段见下）

### 5.5 博客详情 `GET /api/blogs/{id}`

**`data`：**

```json
{
  "blog": { /* Blogs 实体，content 建议为 Markdown 或 HTML；前端用 renderMarkdown 再 v-html */ },
  "comments": [ /* CommentVO 树 */ ],
  "commentCount": 10,
  "authorName": "昵称",
  "authorAvatar": "url",
  "liked": false
}
```

**`CommentVO`：** `id`, `b_id`, `parent_cid`, `u_id`, `author_name`, `author_avatar`, `content`, `created_at`, `children[]`

> 编辑页 `WriteBlogPage` 也用此接口拉取正文；若 `content` 已是 HTML，编辑框会显示 HTML 文本（理想情况编辑接口返回 **原始 Markdown**）。

### 5.6 用户主页 `GET /api/user/profile/{id}`

**`data`：**

```json
{
  "user": { /* Users，含 id, nickname, avatar, intro, gender, exp 等 */ },
  "level": 6,
  "articles": [ /* BlogBriefVO[] */ ],
  "likedBlogs": [ /* BlogBriefVO[] */ ]
}
```

### 5.7 当前用户资料（编辑弹窗）`GET /api/user/profile-data`

需登录。**`data`：** `nickname`, `intro`, `email`, `gender`, `githubUsername`, `avatar`（无 `githubToken` 回显）

### 5.8 归档列表 `GET /api/archives`

需登录，仅当前用户回收站。

**`data`：** `BlogsBin[]`（`id`, `u_id`, `title`, `content`, `created_at`, `updated_at`, `deleted_at`, `t_id`）

### 5.9 归档详情 `GET /api/archives/{id}`（建议实现）

前端 `ReadArchivePage` 优先请求此接口；不存在则从 `GET /api/archives` 列表里按 `id` 查找。

**`data` 建议：**

```json
{
  "archive": { /* BlogsBin */ },
  "authorName": "...",
  "authorAvatar": "..."
}
```

### 5.10 天气 `GET /api/weather/now?location=beijing`

已有 `WeatherController`。**`data`：** `{ "temperature": "25", "text": "晴" }`（字段名需与现网一致）

---

## 6. 写操作 API（多为已有路径，需改为 JWT 识别用户）

| 端点 | 方法 | Content-Type | 参数 | 说明 |
|------|------|--------------|------|------|
| `/blogs/publish` | POST | urlencoded | `title`, `content`, `tagIds?`（逗号分隔 id）, `newTagNames?` | 需登录 |
| `/blogs/update` | POST | urlencoded | `id`, `title`, `content`, `tagIds?`, `newTagNames?` | 需登录 |
| `/blogs/delete/{id}` | POST | — | — | 需登录（详情页未接 UI，可后续补） |
| `/blogs/recover/{id}` | POST | — | — | 归档页「恢复」 |
| `/blogs/like/{id}` | POST | — | — | 需登录 |
| `/blogs/unlike/{id}` | POST | — | — | 需登录 |
| `/blogs/incrementRead/{id}` | POST | — | — | 详情页加载约 500ms 后静默调用 |
| `/user/comment/add` | POST | urlencoded | 见下 | 需登录 |
| `/user/avatar/upload` | POST | `multipart/form-data` | `file` | 需登录；`data` 为新头像 URL |
| `/user/profile/update` | POST | urlencoded | `nickname`, `intro`, `email`, `gender`, `githubUsername`, `githubToken?` | 需登录 |

### 6.1 评论参数 — 与现网 Java 不一致，需后端择一

前端 `CommentSection.vue` 发送：

| 前端参数名 | 含义 |
|-----------|------|
| `bId` | 博客 id |
| `content` | 评论正文 |
| `parentId` | 回复父评论 id（可选） |

**现网 `UserController` 使用：** `blogId`, `content`, `parentCid`

请后端统一为其中一套（推荐 JWT 迁移时改为 `bId` + `parentId`，或在前端改参数名——需单独任务）。

### 6.2 关注列表（沿用现网 JSON）

`GET /user/follow/{followers|following}?userId=&page=&pageSize=20`

**`data`：**

```json
{
  "list": [
    { "user": { "id", "nickname", "avatar", "intro", "level" }, "isFollowing": true }
  ],
  "total": 100
}
```

写操作：`POST /user/follow/{userId}`、`POST /user/unfollow/{userId}`、`POST /user/follow/remove-follower/{userId}`

---

## 7. 静态资源与构建

- 模板内资源路径：`/dist/assets/*`（构建后 hash 会变，需重新 `npm run build` 并同步 templates）
- 头像默认图：`/images/default-avatar.png`
- 天气图标：`/pics/weather/{sunny|cloudy|rainy|snowy|foggy|unknown|loading}.svg`
- Vite 构建 HTML 实际在：`static/dist/src/pages/<page>/index.html`（已脚本同步到 `templates/`）
- Spring Boot 默认 `classpath:/static/` 应能服务 `/dist/assets/**`；若 404 需在 `WebConfiguration` 增加 `/dist/**` 映射（见 JWT 迁移计划 Task 7.1）

---

## 8. 后端待办清单（优先级）

1. **完成 JWT 迁移**：`AuthController` 登录/注册返回 token；`GET /auth/captcha` 使用 `X-Captcha-Token` + Redis（非 sessionId）；`POST /auth/refresh`、`GET /auth/status`
2. **新增 `ApiController`**（`@RequestMapping("/api")`）：实现第 5 节全部 GET
3. **Blog/User 写接口**：从 `HttpSession` 改为 `AuthHelper.getCurrentUser(request)` 解析 JWT
4. **评论参数**：对齐 `bId`/`parentId` 或 `blogId`/`parentCid`
5. **归档详情**：`GET /api/archives/{id}`（可选但推荐）
6. **`GET /api/user/profile-data`**：编辑资料弹窗专用
7. **Security / 拦截器**：`/api/**`、`/auth/**`（除 captcha/login/register）放行规则与 JWT 过滤器一致；页面路由仍可返回 Thymeleaf 壳

---

## 9. 前端文件索引（便于后端对照）

```
src/main/frontend/
├── package.json
├── vite.config.js
└── src/
    ├── utils/token.js, request.js, markdown.js
    ├── stores/auth.js
    ├── shared/components/AppHeader.vue, AuthModal.vue, CommentSection.vue, EditProfileModal.vue, WeatherWidget.vue
    ├── shared/styles/global.scss
    └── pages/{index,readBlog,writeBlog,profile,archives,readArchive,followList,error}/
        ├── index.html, main.js, *Page.vue
```

**已修改模板：** `templates/index.html`, `readBlog.html`, `writeBlog.html`, `profile.html`, `archives.html`, `readArchive.html`, `follow-list.html`, `error.html`

**禁止前端改动的路径（计划约束）：** `src/main/java/**`, `application*.yml`, `pom.xml`, `static/css|js|images|pics/**`

---

## 10. 本地联调命令

```powershell
# 前端开发（代理到 8080）
cd src/main/frontend
npm run dev

# 前端生产构建
npm run build

# 后端
mvn spring-boot:run
# 访问 http://localhost:8080/
```

---

## 11. 已知差异 / 风险

| 项 | 说明 |
|----|------|
| 后端 API 未落地 | 当前无 `ApiController`；页面会空数据或 404 |
| 评论参数字段名 | 见 6.1 |
| 编辑博客正文 | `/api/blogs/{id}` 若返回 HTML，编辑框体验差；建议编辑用 Markdown 字段 |
| 归档 API | 列表已有设计；详情接口可能缺失，前端有 fallback |
| Element 包体积 | `AppHeader` chunk ~1MB（含 Element Plus），后续可拆包优化 |
| captcha | 前端已按 `captchaToken` + 响应头实现；现网仍可能用 `session.getId()` |

---

## 12. 参考文档

- 前端实施计划：`docs/superpowers/plans/2026-05-20-vue3-frontend-implementation.md`
- 后端 JWT + API 计划：`docs/superpowers/plans/2026-05-20-vue3-jwt-migration.md`
- 设计说明：`docs/superpowers/specs/2026-05-20-vue3-jwt-migration-design.md`

---

*文档版本：Ver.1 | 生成日期：2026-05-20*
