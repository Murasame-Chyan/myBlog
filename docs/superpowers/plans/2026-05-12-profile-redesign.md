# 个人主页重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构 `/user/profile` 页面，实现左对齐信息卡（含等级/经验/UID）、文章列表（搜索+排序+分页）、最近点赞展示、编辑资料模态框。

**Architecture:** 后端新增 3 个 API 端点（文章查询、点赞查询、资料更新），前端重写 profile.html + header 模态框 + CSS，移除 settings 页面。等级采用 B 站式阈值计算。

**Tech Stack:** Spring Boot 3.5 + MyBatis + Thymeleaf + Bootstrap 5 + Cropper.js

---

### Task 1: UserService 新增等级计算 + 点赞文章查询

**Files:**
- Modify: `src/main/java/com/murasame/service/UserService.java`
- Modify: `src/main/java/com/murasame/service/impl/UserServiceImpl.java`
- Modify: `src/main/java/com/murasame/mapper/BlogMapper.java`

- [ ] **Step 1: Add `calculateLevel` + `getLikedBlogs` to UserService interface**

Add after `updateProfile`:
```java
int calculateLevel(int exp);

List<com.murasame.domain.vo.BlogBriefVO> getLikedBlogs(Long userId, int limit);
```

- [ ] **Step 2: Add `getBlogsByIds` to BlogMapper**

Add after `countSearchBlogs`:
```java
@ResultMap("com.murasame.mapper.IndexMapper.blogBriefResultMap")
@Select("<script>" +
    "SELECT b.id, b.title, LEFT(b.content,30) AS brief, b.created_at, b.updated_at, " +
    "u.nickname AS author, b.t_id, b.read_count, b.like_count, " +
    "COALESCE(c.comment_count,0) AS comment_count " +
    "FROM blogs b " +
    "LEFT JOIN users u ON b.u_id=u.id " +
    "LEFT JOIN (SELECT b_id, COUNT(*) AS comment_count FROM comments GROUP BY b_id) c ON b.id=c.b_id " +
    "WHERE b.id IN " +
    "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
    " ORDER BY b.created_at DESC" +
    "</script>")
List<com.murasame.domain.vo.BlogBriefVO> getBlogsByIds(@Param("ids") java.util.List<Long> ids);
```

- [ ] **Step 3: Implement in UserServiceImpl**

Add import:
```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.fasterxml.jackson.core.type.TypeReference;
import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.mapper.BlogMapper;
```

Add field:
```java
@Resource
private BlogMapper blogMapper;
```

Add methods at end of class (before closing `}`):
```java
private static final long[] LEVEL_THRESHOLDS = {
    0, 200, 1500, 4500, 10800, 28800, 65000, 140000, 300000, 600000
};

@Override
public int calculateLevel(int exp) {
    int level = 1;
    for (int i = 1; i < LEVEL_THRESHOLDS.length; i++) {
        if (exp >= LEVEL_THRESHOLDS[i]) {
            level = i + 1;
        } else {
            break;
        }
    }
    return level;
}

@Override
public List<BlogBriefVO> getLikedBlogs(Long userId, int limit) {
    if (userId == null) return Collections.emptyList();
    Users user = userMapper.getUserById(userId);
    if (user == null || user.getLiked_b_id() == null || user.getLiked_b_id().isEmpty()) {
        return Collections.emptyList();
    }
    try {
        List<Long> likedIds = objectMapper.readValue(user.getLiked_b_id(), new TypeReference<>() {});
        List<Long> recent = new ArrayList<>();
        for (int i = Math.max(0, likedIds.size() - limit); i < likedIds.size(); i++) {
            recent.add(likedIds.get(i));
        }
        Collections.reverse(recent);
        if (recent.isEmpty()) return Collections.emptyList();
        return blogMapper.getBlogsByIds(recent);
    } catch (Exception e) {
        return Collections.emptyList();
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/murasame/service/UserService.java src/main/java/com/murasame/service/impl/UserServiceImpl.java src/main/java/com/murasame/mapper/BlogMapper.java
git commit -m "feat: add calculateLevel, getLikedBlogs, getBlogsByIds"
```

---

### Task 2: BlogService/Mapper 新增用户文章查询

**Files:**
- Modify: `src/main/java/com/murasame/mapper/BlogMapper.java`
- Modify: `src/main/java/com/murasame/service/BlogService.java`
- Modify: `src/main/java/com/murasame/service/impl/BlogServiceImpl.java`

- [ ] **Step 1: Add user-specific query methods to BlogMapper**

Add after `countSearchBlogs`:
```java
@ResultMap("com.murasame.mapper.IndexMapper.blogBriefResultMap")
@Select("<script>" +
    "SELECT b.id, b.title, LEFT(b.content,30) AS brief, b.created_at, b.updated_at, " +
    "u.nickname AS author, b.t_id, b.read_count, b.like_count, " +
    "COALESCE(c.comment_count,0) AS comment_count " +
    "FROM blogs b " +
    "LEFT JOIN users u ON b.u_id=u.id " +
    "LEFT JOIN (SELECT b_id, COUNT(*) AS comment_count FROM comments GROUP BY b_id) c ON b.id=c.b_id " +
    "WHERE b.u_id=#{userId} " +
    "<if test='keyword != null and keyword != \"\"'>" +
    "AND b.title LIKE CONCAT('%',#{keyword},'%') " +
    "</if>" +
    "<choose>" +
    "<when test='sortBy == \"reads\"'>ORDER BY b.read_count DESC</when>" +
    "<when test='sortBy == \"likes\"'>ORDER BY b.like_count DESC</when>" +
    "<otherwise>ORDER BY b.created_at DESC</otherwise>" +
    "</choose>" +
    "LIMIT #{pageSize} OFFSET #{offset}" +
    "</script>")
List<BlogBriefVO> getBlogsByUserId(@Param("userId") Long userId,
                                    @Param("keyword") String keyword,
                                    @Param("sortBy") String sortBy,
                                    @Param("pageSize") int pageSize,
                                    @Param("offset") int offset);

@Select("<script>" +
    "SELECT COUNT(*) FROM blogs b WHERE b.u_id=#{userId} " +
    "<if test='keyword != null and keyword != \"\"'>" +
    "AND b.title LIKE CONCAT('%',#{keyword},'%') " +
    "</if>" +
    "</script>")
long countBlogsByUserId(@Param("userId") Long userId, @Param("keyword") String keyword);
```

- [ ] **Step 2: Add to BlogService interface**

Add after `countSearchBlogs`:
```java
List<BlogBriefVO> getUserBlogs(Long userId, String keyword, String sortBy, int page, int pageSize);

long countUserBlogs(Long userId, String keyword);
```

- [ ] **Step 3: Implement in BlogServiceImpl**

Add at end of class:
```java
@Override
public List<BlogBriefVO> getUserBlogs(Long userId, String keyword, String sortBy, int page, int pageSize) {
    int offset = (page - 1) * pageSize;
    return blogMapper.getBlogsByUserId(userId, keyword, sortBy, pageSize, offset);
}

@Override
public long countUserBlogs(Long userId, String keyword) {
    return blogMapper.countBlogsByUserId(userId, keyword);
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/murasame/mapper/BlogMapper.java src/main/java/com/murasame/service/BlogService.java src/main/java/com/murasame/service/impl/BlogServiceImpl.java
git commit -m "feat: add user-specific blog query methods"
```

---

### Task 3: Update UserController (profile route, new APIs, remove settings)

**Files:**
- Modify: `src/main/java/com/murasame/controller/UserController.java`

- [ ] **Step 1: Add imports**

Add after existing imports:
```java
import com.murasame.domain.vo.BlogBriefVO;
import java.util.HashMap;
import java.util.Collections;
```

- [ ] **Step 2: Replace the `profile` method**

Replace the existing `@GetMapping("/profile")` method:
```java
@GetMapping("/profile")
public String profile(@RequestParam(required = false) Long id,
                      HttpSession session, Model model) {
    Users currentUser = (Users) session.getAttribute("currentUser");
    if (currentUser == null) {
        return "redirect:/";
    }
    Long profileUserId = (id != null) ? id : currentUser.getId();
    Users profileUser = userService.getUserById(profileUserId);
    if (profileUser == null) {
        return "redirect:/";
    }
    model.addAttribute("profileUser", profileUser);
    model.addAttribute("isOwner", currentUser.getId().equals(profileUserId));
    model.addAttribute("githubUsername", profileUser.getGithubUsername());
    return "profile";
}
```

- [ ] **Step 3: Add `updateProfile` POST endpoint**

Add after the new `profile` method:
```java
@ResponseBody
@PostMapping("/profile/update")
public Map<String, Object> updateProfile(
        @NotBlank(message = "昵称不能为空")
        @Size(max = 32, message = "昵称不能超过32个字符")
        @RequestParam String nickname,
        @Size(max = 255, message = "简介不能超过255个字符")
        @RequestParam(required = false) String intro,
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 255, message = "邮箱不能超过255个字符")
        @RequestParam String email,
        @RequestParam Integer gender,
        @Size(max = 255, message = "GitHub用户名不能超过255个字符")
        @RequestParam(required = false) String githubUsername,
        HttpSession session) {
    Users currentUser = (Users) session.getAttribute("currentUser");
    if (currentUser == null) {
        return ReturnUtil.unauthorized("请先登录");
    }
    try {
        Users user = new Users();
        user.setId(currentUser.getId());
        user.setNickname(nickname);
        user.setIntro(intro);
        user.setEmail(email);
        user.setGender(gender);
        user.setGithubUsername(githubUsername);
        Users updated = userService.updateProfile(user);
        session.setAttribute("currentUser", updated);
        return ReturnUtil.success("保存成功");
    } catch (IllegalArgumentException e) {
        return ReturnUtil.error(e.getMessage());
    }
}
```

- [ ] **Step 4: Add user blogs API**

Add after `updateProfile`:
```java
@ResponseBody
@GetMapping("/profile/blogs")
public Map<String, Object> getUserBlogs(
        @RequestParam Long userId,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "newest") String sortBy,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize) {
    List<BlogBriefVO> list = blogService.getUserBlogs(userId,
            keyword != null && keyword.isBlank() ? null : keyword,
            sortBy, page, pageSize);
    long total = blogService.countUserBlogs(userId,
            keyword != null && keyword.isBlank() ? null : keyword);
    Map<String, Object> data = new HashMap<>();
    data.put("list", list);
    data.put("total", total);
    data.put("page", page);
    data.put("pageSize", pageSize);
    return ReturnUtil.success("获取成功", data);
}
```

Add `BlogService` field injection:
```java
@Resource
private BlogService blogService;
```

- [ ] **Step 5: Add liked blogs API**

Add after `getUserBlogs`:
```java
@ResponseBody
@GetMapping("/profile/likes")
public Map<String, Object> getLikedBlogs(@RequestParam Long userId, HttpSession session) {
    Users currentUser = (Users) session.getAttribute("currentUser");
    if (currentUser == null || !currentUser.getId().equals(userId)) {
        return ReturnUtil.success("获取成功", Collections.emptyList());
    }
    List<BlogBriefVO> likedBlogs = userService.getLikedBlogs(userId, 10);
    return ReturnUtil.success("获取成功", likedBlogs);
}
```

- [ ] **Step 6: Remove settings routes**

Delete these two methods entirely: `@GetMapping("/settings")` and `@PostMapping("/settings")`.

Also update imports — remove `@Email`, `@NotBlank`, `@Size` if no longer used by remaining methods. (They are still used in `updateProfile`, so keep them.)

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/murasame/controller/UserController.java
git commit -m "feat: update profile controller with new APIs, remove settings routes"
```

---

### Task 4: Add profile page CSS styles

**Files:**
- Modify: `src/main/resources/static/css/common.css`

- [ ] **Step 1: Append profile styles to common.css**

Append at end of file:
```css
/* ===== 个人主页 ===== */
.profile-card {
    background: rgba(255,255,255,0.08);
    backdrop-filter: blur(12px);
    border: 1px solid rgba(135,206,235,0.15);
    border-radius: 16px;
    padding: 28px;
    display: flex;
    align-items: center;
    gap: 24px;
}
.profile-avatar-section {
    flex-shrink: 0;
    text-align: center;
}
.profile-avatar-img {
    width: 80px;
    height: 80px;
    border-radius: 50%;
    object-fit: cover;
    border: 3px solid rgba(135,206,235,0.35);
}
.profile-uid {
    font-size: 0.72rem;
    color: rgba(255,255,255,0.35);
    margin-top: 6px;
}
.profile-info {
    flex: 1;
    min-width: 0;
}
.profile-nickname-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 4px;
}
.profile-nickname {
    font-size: 1.3rem;
    font-weight: bold;
    color: #fff;
}
.profile-lv-badge {
    display: inline-block;
    background: linear-gradient(135deg, #FF6B9D, #D4467A);
    color: #fff;
    font-size: 0.65rem;
    font-weight: 700;
    padding: 2px 7px;
    border-radius: 3px;
    letter-spacing: 0.5px;
    line-height: 1.2;
}
.profile-meta {
    font-size: 0.82rem;
    color: rgba(255,255,255,0.45);
    margin-bottom: 8px;
}
.profile-exp-bar-wrap {
    width: 220px;
    height: 6px;
    background: rgba(255,255,255,0.08);
    border-radius: 3px;
    overflow: hidden;
    margin-bottom: 2px;
}
.profile-exp-bar-fill {
    height: 100%;
    background: linear-gradient(90deg, #87CEEB, #6BB5D9);
    border-radius: 3px;
    transition: width 0.4s ease;
}
.profile-exp-text {
    font-size: 0.68rem;
    color: rgba(255,255,255,0.3);
}
.profile-follow-btn {
    flex-shrink: 0;
    display: flex;
    align-items: center;
}
.btn-follow {
    background: linear-gradient(135deg, #FF6B9D, #D4467A);
    border: none;
    color: #fff;
    padding: 8px 24px;
    border-radius: 20px;
    font-size: 0.85rem;
    font-weight: 600;
    cursor: pointer;
    white-space: nowrap;
    transition: all 0.3s;
}
.btn-follow:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 15px rgba(255,107,157,0.4);
    color: #fff;
}

/* 文章区块 */
.profile-section {
    background: rgba(255,255,255,0.06);
    backdrop-filter: blur(8px);
    border: 1px solid rgba(135,206,235,0.12);
    border-radius: 14px;
    padding: 20px;
    margin-top: 20px;
}
.profile-section-title {
    color: #fff;
    font-weight: 600;
    font-size: 0.95rem;
    margin-bottom: 14px;
    padding-bottom: 10px;
    border-bottom: 1px solid rgba(135,206,235,0.1);
}
.profile-section-title i {
    color: #87CEEB;
    margin-right: 6px;
}

/* 排序按钮组 */
.sort-btn-group {
    display: flex;
    gap: 6px;
}
.sort-btn {
    font-size: 0.78rem;
    padding: 5px 14px;
    border-radius: 16px;
    background: rgba(255,255,255,0.05);
    border: 1px solid rgba(135,206,235,0.12);
    color: rgba(255,255,255,0.5);
    cursor: pointer;
    transition: all 0.25s;
    white-space: nowrap;
}
.sort-btn:hover {
    background: rgba(135,206,235,0.1);
    color: rgba(255,255,255,0.8);
}
.sort-btn.active {
    background: rgba(135,206,235,0.2);
    border-color: #87CEEB;
    color: #87CEEB;
    font-weight: 600;
}

/* 文章列表 */
.article-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 14px;
    border-bottom: 1px solid rgba(255,255,255,0.04);
    transition: background 0.2s;
}
.article-item:hover {
    background: rgba(255,255,255,0.03);
}
.article-title-link {
    color: rgba(255,255,255,0.7);
    text-decoration: none;
    font-size: 0.87rem;
    transition: color 0.2s;
}
.article-title-link:hover {
    color: #87CEEB;
}
.article-meta-inline {
    font-size: 0.7rem;
    color: rgba(255,255,255,0.3);
    white-space: nowrap;
    flex-shrink: 0;
}

/* 文章分页 */
.profile-pagination {
    display: flex;
    justify-content: center;
    gap: 6px;
    margin-top: 14px;
}
.profile-page-btn {
    padding: 4px 10px;
    border-radius: 6px;
    background: rgba(255,255,255,0.04);
    border: 1px solid transparent;
    color: rgba(255,255,255,0.5);
    font-size: 0.8rem;
    cursor: pointer;
    transition: all 0.2s;
}
.profile-page-btn:hover {
    background: rgba(135,206,235,0.12);
    color: #87CEEB;
}
.profile-page-btn.active {
    background: rgba(135,206,235,0.15);
    border-color: rgba(135,206,235,0.3);
    color: #87CEEB;
    font-weight: 600;
}
.profile-page-btn.disabled {
    color: rgba(255,255,255,0.15);
    cursor: default;
    pointer-events: none;
}

/* 点赞区块（仅自己可见） */
.liked-section {
    background: rgba(255,107,157,0.04);
    border: 1px dashed rgba(255,107,157,0.18);
}
.liked-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    background: rgba(255,255,255,0.03);
    border-radius: 8px;
    margin-bottom: 6px;
    transition: background 0.2s;
}
.liked-item:hover {
    background: rgba(255,255,255,0.06);
}

/* 编辑资料模态框 */
.edit-profile-overlay {
    display: none;
    position: fixed;
    inset: 0;
    background: rgba(0,0,0,0.65);
    backdrop-filter: blur(4px);
    -webkit-backdrop-filter: blur(4px);
    z-index: 9999;
    align-items: center;
    justify-content: center;
}
.edit-profile-overlay.active {
    display: flex;
}
.edit-profile-modal {
    background: #1a1e2b;
    border: 1px solid rgba(135,206,235,0.2);
    border-radius: 16px;
    width: 480px;
    max-width: 94vw;
    max-height: 90vh;
    overflow-y: auto;
    box-shadow: 0 20px 60px rgba(0,0,0,0.5);
}
.edit-profile-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 20px;
    border-bottom: 1px solid rgba(135,206,235,0.12);
    position: sticky;
    top: 0;
    background: #1a1e2b;
    border-radius: 16px 16px 0 0;
    z-index: 1;
}
.edit-profile-header h5 {
    color: #fff;
    font-weight: 600;
    margin: 0;
}
.edit-profile-close {
    background: none;
    border: none;
    color: rgba(255,255,255,0.4);
    font-size: 1.2rem;
    cursor: pointer;
    transition: color 0.2s;
}
.edit-profile-close:hover { color: #fff; }
.edit-profile-body {
    padding: 20px;
}
.edit-profile-field {
    margin-bottom: 16px;
}
.edit-profile-label {
    color: rgba(255,255,255,0.7);
    font-weight: 600;
    font-size: 0.82rem;
    margin-bottom: 6px;
}
.edit-profile-input {
    width: 100%;
    background: rgba(255,255,255,0.06);
    border: 1px solid rgba(135,206,235,0.18);
    border-radius: 10px;
    color: #fff;
    padding: 10px 14px;
    font-size: 0.88rem;
    outline: none;
    transition: all 0.25s;
    box-sizing: border-box;
}
.edit-profile-input:focus {
    background: rgba(255,255,255,0.1);
    border-color: rgba(135,206,235,0.5);
    box-shadow: 0 0 0 3px rgba(135,206,235,0.08);
}
.edit-profile-input::placeholder {
    color: rgba(255,255,255,0.25);
}
.edit-profile-select {
    width: 100%;
    background: rgba(255,255,255,0.06);
    border: 1px solid rgba(135,206,235,0.18);
    border-radius: 10px;
    color: #fff;
    padding: 10px 14px;
    font-size: 0.88rem;
    outline: none;
}
.edit-profile-select option {
    background: #1a1e2b;
    color: #fff;
}
.edit-profile-save-btn {
    width: 100%;
    padding: 11px;
    border: none;
    border-radius: 22px;
    background: linear-gradient(135deg, #87CEEB, #6BB5D9);
    color: #fff;
    font-weight: 600;
    font-size: 0.9rem;
    cursor: pointer;
    transition: all 0.3s;
    margin-top: 4px;
}
.edit-profile-save-btn:hover {
    box-shadow: 0 4px 15px rgba(135,206,235,0.4);
    transform: translateY(-1px);
}
.edit-profile-save-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

/* 编辑资料 — 头像预览 */
.edit-avatar-area {
    display: flex;
    align-items: center;
    gap: 14px;
}
.edit-avatar-preview {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    object-fit: cover;
    border: 2px solid rgba(135,206,235,0.3);
    flex-shrink: 0;
}
.edit-avatar-upload-btn {
    background: rgba(135,206,235,0.15);
    border: 1px solid rgba(135,206,235,0.25);
    border-radius: 18px;
    color: #87CEEB;
    padding: 6px 16px;
    font-size: 0.82rem;
    cursor: pointer;
    transition: all 0.25s;
}
.edit-avatar-upload-btn:hover {
    background: rgba(135,206,235,0.25);
}

/* 裁剪弹窗 (复用自 settings) */
.crop-overlay {
    display: none;
    position: fixed;
    inset: 0;
    background: rgba(0,0,0,0.8);
    z-index: 99999;
    align-items: center;
    justify-content: center;
    backdrop-filter: blur(6px);
}
.crop-overlay.active { display: flex; }
.crop-modal {
    background: #1a1e2b;
    border: 1px solid rgba(135,206,235,0.2);
    border-radius: 16px;
    overflow: hidden;
    width: 560px;
    max-width: 96vw;
    box-shadow: 0 20px 60px rgba(0,0,0,0.5);
}
.crop-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    color: #fff;
    font-weight: 600;
    font-size: 0.95rem;
    border-bottom: 1px solid rgba(255,255,255,0.06);
}
.crop-close {
    background: none;
    border: none;
    color: rgba(255,255,255,0.4);
    font-size: 1.2rem;
    cursor: pointer;
    padding: 2px 8px;
    transition: color 0.2s;
}
.crop-close:hover { color: #fff; }
.crop-body {
    display: flex;
    max-height: 55vh;
}
.crop-area {
    flex: 1;
    min-height: 280px;
    background: rgba(0,0,0,0.2);
    display: flex;
    align-items: center;
    justify-content: center;
}
.crop-area img {
    max-width: 100%;
    max-height: 100%;
    display: block;
}
.crop-preview-side {
    width: 120px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 12px;
    border-left: 1px solid rgba(255,255,255,0.06);
    flex-shrink: 0;
}
.preview-circle {
    width: 80px;
    height: 80px;
    border-radius: 50%;
    overflow: hidden;
    border: 2px solid rgba(135,206,235,0.25);
}
.crop-footer {
    padding: 10px 16px;
    border-top: 1px solid rgba(255,255,255,0.06);
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    flex-wrap: wrap;
}
.crop-tools {
    display: flex;
    gap: 4px;
}
.crop-tools button {
    width: 32px; height: 32px;
    border-radius: 50%;
    border: 1px solid rgba(255,255,255,0.12);
    background: rgba(255,255,255,0.06);
    color: rgba(255,255,255,0.6);
    cursor: pointer;
    font-size: 0.85rem;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;
    padding: 0;
}
.crop-tools button:hover {
    background: rgba(255,255,255,0.14);
    color: #fff;
}
.crop-actions {
    display: flex;
    gap: 10px;
}
.crop-btn {
    padding: 8px 22px;
    border-radius: 20px;
    border: none;
    font-weight: 600;
    font-size: 0.85rem;
    cursor: pointer;
    transition: all 0.25s;
}
.crop-btn-cancel {
    background: rgba(255,255,255,0.08);
    color: rgba(255,255,255,0.6);
}
.crop-btn-cancel:hover { background: rgba(255,255,255,0.14); color: #fff; }
.crop-btn-confirm {
    background: linear-gradient(135deg, #87CEEB 0%, #6BB5D9 100%);
    color: #fff;
}
.crop-btn-confirm:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 14px rgba(135,206,235,0.35);
}
@media (max-width: 500px) {
    .crop-preview-side { display: none; }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/css/common.css
git commit -m "style: add profile page and edit modal CSS"
```

---

### Task 5: Rewrite profile.html

**Files:**
- Modify: `src/main/resources/templates/profile.html`

- [ ] **Step 1: Write the new profile.html**

```html
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <title>个人主页</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css">
    <link rel="stylesheet" href="/css/common.css">
</head>
<body>
    <header th:replace="~{fragment/header :: nav('index')}"></header>

    <main class="container my-5">
        <div class="row justify-content-center">
            <div class="col-lg-9">

                <!-- 个人信息卡片 -->
                <div class="profile-card">
                    <!-- 左：头像 + UID -->
                    <div class="profile-avatar-section">
                        <img th:if="${profileUser.avatar != null}"
                             th:src="${profileUser.avatar}"
                             alt="头像" class="profile-avatar-img">
                        <img th:if="${profileUser.avatar == null}"
                             src="/images/default-avatar.png"
                             alt="默认头像" class="profile-avatar-img">
                        <div class="profile-uid" th:text="'UID：' + ${profileUser.id}">UID：10001</div>
                    </div>
                    <!-- 中：昵称 + LV + 性别 + 经验条 -->
                    <div class="profile-info">
                        <div class="profile-nickname-row">
                            <span class="profile-nickname" th:text="${profileUser.nickname}">用户昵称</span>
                            <span class="profile-lv-badge" th:text="'LV.' + ${profileUser.level != null ? profileUser.level : 1}">LV.6</span>
                        </div>
                        <div class="profile-meta">
                            <span th:if="${profileUser.gender == 1}">♂ 男</span>
                            <span th:if="${profileUser.gender == 2}">♀ 女</span>
                            <span th:if="${profileUser.gender == null or profileUser.gender == 0}">⚥ 未设置</span>
                            <span th:if="${profileUser.exp != null}">
                                &nbsp;·&nbsp; 经验 <span th:text="${profileUser.exp}">35000</span> / 100000
                            </span>
                        </div>
                        <div class="profile-exp-bar-wrap" th:if="${profileUser.exp != null}">
                            <div class="profile-exp-bar-fill"
                                 th:style="'width:' + (${profileUser.exp} > 100000 ? 100 : ${profileUser.exp} / 1000.0) + '%'"></div>
                        </div>
                        <div class="profile-exp-text" th:if="${profileUser.exp != null}">
                            距下一级还需 <span th:text="${100000 - profileUser.exp > 0 ? 100000 - profileUser.exp : 0}">65000</span> 经验
                        </div>
                        <div class="profile-exp-text" th:if="${profileUser.exp == null}">
                            暂无经验值数据
                        </div>
                    </div>
                    <!-- 右：关注按钮 -->
                    <div class="profile-follow-btn" th:if="${!isOwner}">
                        <button class="btn-follow" onclick="alert('功能开发中')">+ 关注</button>
                    </div>
                </div>

                <!-- 我的文章 -->
                <div class="profile-section">
                    <div class="profile-section-title">
                        <i class="bi bi-file-text"></i> 我的文章
                    </div>
                    <!-- 搜索 + 排序 -->
                    <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 14px; flex-wrap: wrap;">
                        <div style="flex: 1; min-width: 180px; position: relative;">
                            <i class="bi bi-search" style="position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: rgba(255,255,255,0.35); font-size: 0.85rem; pointer-events: none; z-index: 2;"></i>
                            <input type="text" id="articleSearch"
                                   placeholder="搜索文章标题..."
                                   class="search-input"
                                   style="width: 100%; padding: 8px 14px 8px 36px; background: rgba(255,255,255,0.06); border: 1px solid rgba(135,206,235,0.18); border-radius: 20px; color: #fff; font-size: 0.85rem; outline: none; box-sizing: border-box;"
                                   onkeydown="if(event.key==='Enter') loadArticles(1)">
                        </div>
                        <div class="sort-btn-group">
                            <button class="sort-btn active" data-sort="newest" onclick="switchSort('newest', this)">最新发布</button>
                            <button class="sort-btn" data-sort="reads" onclick="switchSort('reads', this)">最多阅读</button>
                            <button class="sort-btn" data-sort="likes" onclick="switchSort('likes', this)">最多点赞</button>
                        </div>
                    </div>
                    <!-- 文章列表 -->
                    <div id="articleList"></div>
                    <!-- 分页 -->
                    <div class="profile-pagination" id="articlePagination"></div>
                </div>

                <!-- 最近点赞（仅自己可见） -->
                <div class="profile-section liked-section" id="likedSection" style="display: none;">
                    <div class="profile-section-title">
                        <i class="bi bi-heart-fill" style="color: #FF6B9D;"></i> 最近点赞的文章
                        <span style="font-size: 0.7rem; color: #FF6B9D; font-weight: 400; margin-left: 4px;">（仅自己可见）</span>
                    </div>
                    <div id="likedList"></div>
                </div>

                <!-- GitHub 展示区块 -->
                <div class="my-4" id="github-section">
                    <div class="github-card text-center py-4" id="github-unlinked" style="display: none;">
                        <i class="bi bi-github" style="font-size: 2.5rem; color: rgba(255,255,255,0.2);"></i>
                        <p class="mt-3 text-muted">未绑定 GitHub 账号</p>
                        <p style="font-size: 0.82rem; color: rgba(255,255,255,0.3);">在编辑资料中绑定 GitHub 用户名即可展示贡献热力图、仓库和成就</p>
                    </div>
                    <div class="text-center py-4" id="github-loading" style="display: none;">
                        <div class="spinner-border" role="status" style="color: #87CEEB; width: 2rem; height: 2rem;"></div>
                        <p class="mt-2 text-muted">正在获取 GitHub 数据...</p>
                    </div>
                    <div class="text-center py-4" id="github-error" style="display: none;">
                        <i class="bi bi-exclamation-triangle" style="font-size: 2rem; color: #ff6b6b;"></i>
                        <p class="mt-2" style="color: #ff6b6b;" id="github-error-msg"></p>
                        <button class="btn btn-sm mt-2" onclick="retryGitHub()" style="background: rgba(255,255,255,0.1); color: #fff; border: 1px solid rgba(255,255,255,0.2); border-radius: 20px;">
                            <i class="bi bi-arrow-clockwise"></i> 重试
                        </button>
                    </div>
                    <div class="github-card" id="github-heatmap-card" style="display: none;">
                        <h5 class="github-section-title"><i class="bi bi-grid-3x3-gap-fill"></i> 贡献热力图</h5>
                        <div class="skeleton-block" id="heatmap-skeleton">
                            <div class="skeleton-line skeleton-line-lg" style="width: 60%;"></div>
                            <div class="skeleton-grid"></div>
                        </div>
                        <div id="heatmap-content">
                            <p class="text-muted" style="font-size: 0.85rem;" id="github-total-contributions"></p>
                            <div class="heatmap-container" id="heatmap-grid"></div>
                            <div class="heatmap-legend">
                                <span style="font-size: 0.72rem; color: rgba(255,255,255,0.4);">Less</span>
                                <span class="heatmap-cell" style="background: rgba(255,255,255,0.06);"></span>
                                <span class="heatmap-cell" style="background: #9be9a8;"></span>
                                <span class="heatmap-cell" style="background: #40c463;"></span>
                                <span class="heatmap-cell" style="background: #30a14e;"></span>
                                <span class="heatmap-cell" style="background: #216e39;"></span>
                                <span style="font-size: 0.72rem; color: rgba(255,255,255,0.4);">More</span>
                            </div>
                        </div>
                        <div class="section-error" id="heatmap-error" style="display: none;">
                            <i class="bi bi-exclamation-circle" style="color: #ff6b6b;"></i>
                            <span style="color: rgba(255,255,255,0.5); font-size: 0.85rem;">热力图加载失败</span>
                        </div>
                        <div class="section-empty" id="heatmap-empty" style="display: none;">
                            <p class="text-muted" style="font-size: 0.85rem;">暂未获取到贡献数据</p>
                        </div>
                    </div>
                    <div class="github-card" id="github-achievements-card" style="display: none;">
                        <h5 class="github-section-title"><i class="bi bi-trophy-fill"></i> 成就徽章</h5>
                        <div class="skeleton-block" id="achievements-skeleton">
                            <div class="skeleton-row">
                                <div class="skeleton-avatar"></div>
                                <div class="skeleton-avatar"></div>
                                <div class="skeleton-avatar"></div>
                                <div class="skeleton-avatar"></div>
                            </div>
                        </div>
                        <div class="achievements-grid" id="achievements-grid"></div>
                        <div class="section-error" id="achievements-error" style="display: none;">
                            <i class="bi bi-exclamation-circle" style="color: #ff6b6b;"></i>
                            <span style="color: rgba(255,255,255,0.5); font-size: 0.85rem;">成就加载失败</span>
                        </div>
                        <div class="section-empty" id="achievements-empty" style="display: none;">
                            <p class="text-muted">暂无成就徽章</p>
                        </div>
                    </div>
                    <div class="github-card" id="github-repos-card" style="display: none;">
                        <h5 class="github-section-title"><i class="bi bi-journal-code"></i> 仓库列表</h5>
                        <div class="skeleton-block" id="repos-skeleton">
                            <div class="skeleton-line" style="width: 40%;"></div>
                            <div class="skeleton-line" style="width: 70%;"></div>
                            <div class="skeleton-line" style="width: 55%;"></div>
                        </div>
                        <div id="repos-list"></div>
                        <div class="section-error" id="repos-error" style="display: none;">
                            <i class="bi bi-exclamation-circle" style="color: #ff6b6b;"></i>
                            <span style="color: rgba(255,255,255,0.5); font-size: 0.85rem;">仓库列表加载失败</span>
                        </div>
                        <div class="section-empty" id="repos-empty" style="display: none;">
                            <p class="text-muted">暂无公开仓库</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </main>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
    <script src="/js/github.js"></script>
    <script th:inline="javascript">
        var profileUserId = /*[[${profileUser.id}]]*/ 0;
        var isOwner = /*[[${isOwner}]]*/ false;
        var currentSort = 'newest';
        var currentPage = 1;
        var totalPages = 1;

        document.addEventListener('DOMContentLoaded', function() {
            loadArticles(1);
            if (isOwner) {
                document.getElementById('likedSection').style.display = 'block';
                loadLiked();
            }
            // GitHub
            var githubUsername = /*[[${githubUsername}]]*/ '';
            if (githubUsername != null && githubUsername.trim() !== '') {
                document.getElementById('github-heatmap-card').style.display = 'block';
                document.getElementById('github-achievements-card').style.display = 'block';
                document.getElementById('github-repos-card').style.display = 'block';
                document.getElementById('heatmap-skeleton').style.display = 'block';
                document.getElementById('achievements-skeleton').style.display = 'block';
                document.getElementById('repos-skeleton').style.display = 'block';
                loadGitHubProfile(githubUsername);
            } else {
                document.getElementById('github-unlinked').style.display = 'block';
            }
        });

        function switchSort(sort, btn) {
            currentSort = sort;
            document.querySelectorAll('.sort-btn').forEach(function(b) { b.classList.remove('active'); });
            btn.classList.add('active');
            loadArticles(1);
        }

        function loadArticles(page) {
            currentPage = page;
            var keyword = document.getElementById('articleSearch').value.trim();
            var params = 'userId=' + profileUserId + '&sortBy=' + currentSort + '&page=' + page + '&pageSize=10';
            if (keyword) params += '&keyword=' + encodeURIComponent(keyword);

            fetch('/user/profile/blogs?' + params)
                .then(function(res) { return res.json(); })
                .then(function(data) {
                    if (data.code !== 200) { document.getElementById('articleList').innerHTML = '<p class="text-muted">加载失败</p>'; return; }
                    var list = data.data.list;
                    totalPages = Math.ceil(data.data.total / data.data.pageSize) || 1;
                    var html = '';
                    if (list.length === 0) {
                        html = '<p class="text-muted" style="text-align:center;padding:20px 0;">暂无文章</p>';
                    } else {
                        list.forEach(function(blog) {
                            html += '<div class="article-item">' +
                                '<a class="article-title-link" href="/blogs/read/' + blog.id + '">' + escapeHtml(blog.title) + '</a>' +
                                '<span class="article-meta-inline">' +
                                ' 📖 ' + (blog.read_count || 0) +
                                ' &nbsp; 👍 ' + (blog.like_count || 0) +
                                ' &nbsp; ' + formatDate(blog.created_at) +
                                '</span></div>';
                        });
                    }
                    document.getElementById('articleList').innerHTML = html;
                    renderPagination();
                })
                .catch(function() {
                    document.getElementById('articleList').innerHTML = '<p class="text-muted">加载失败</p>';
                });
        }

        function renderPagination() {
            var html = '';
            html += '<button class="profile-page-btn' + (currentPage <= 1 ? ' disabled' : '') + '" onclick="if(' + currentPage + '>1)loadArticles(' + (currentPage - 1) + ')">‹</button>';
            for (var i = 1; i <= totalPages; i++) {
                html += '<button class="profile-page-btn' + (i === currentPage ? ' active' : '') + '" onclick="loadArticles(' + i + ')">' + i + '</button>';
            }
            html += '<button class="profile-page-btn' + (currentPage >= totalPages ? ' disabled' : '') + '" onclick="if(' + currentPage + '<' + totalPages + ')loadArticles(' + (currentPage + 1) + ')">›</button>';
            document.getElementById('articlePagination').innerHTML = html;
        }

        function loadLiked() {
            fetch('/user/profile/likes?userId=' + profileUserId)
                .then(function(res) { return res.json(); })
                .then(function(data) {
                    var list = data.data || [];
                    var html = '';
                    if (list.length === 0) {
                        html = '<p class="text-muted" style="text-align:center;padding:12px 0;">暂无点赞记录</p>';
                    } else {
                        list.forEach(function(blog) {
                            html += '<div class="liked-item">' +
                                '<a class="article-title-link" href="/blogs/read/' + blog.id + '">' + escapeHtml(blog.title) + '</a>' +
                                '<span style="color:rgba(255,255,255,0.25);font-size:0.7rem;">' + formatDate(blog.created_at) + '</span></div>';
                        });
                    }
                    document.getElementById('likedList').innerHTML = html;
                })
                .catch(function() {
                    document.getElementById('likedList').innerHTML = '<p class="text-muted">加载失败</p>';
                });
        }

        function escapeHtml(str) {
            var div = document.createElement('div');
            div.textContent = str;
            return div.innerHTML;
        }

        function formatDate(dateStr) {
            if (!dateStr) return '';
            return dateStr.substring(0, 10);
        }
    </script>
</body>
</html>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/templates/profile.html
git commit -m "feat: rewrite profile page with new layout, articles, likes"
```

---

### Task 6: Add edit profile modal to header.html

**Files:**
- Modify: `src/main/resources/templates/fragment/header.html`

- [ ] **Step 1: Add cropper CSS to header head, and "edit profile" to dropdown**

In the `<style>` block (or right before `</style>`), ensure the edit-profile CSS classes are available (they're now in common.css, but add cropper CDN link).

In the `<head>` area of header.html (this is a fragment, so it goes into the parent page's head — but the cropper is needed for the edit modal). Add the cropper CSS CDN as a `<link>` within the `<th:block>`:
```html
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/cropperjs@1.6.2/dist/cropper.min.css">
```

In the dropdown menu, after "账号设置" (which should be removed) and before the divider before logout, add:
```html
<a class="dropdown-item" onclick="openEditProfileModal()" style="cursor: pointer;">
    <i class="bi bi-pencil-square"></i> 编辑资料
</a>
```

And remove the existing settings link:
Delete these lines:
```html
<a th:href="@{/user/settings}" class="dropdown-item">
    <i class="bi bi-gear"></i> 账号设置
</a>
```

- [ ] **Step 2: Add edit profile modal before the existing auth overlay**

Before `<div class="auth-overlay" id="authOverlay">`, add:

```html
<!-- 编辑资料模态框 -->
<div class="edit-profile-overlay" id="editProfileOverlay">
    <div class="edit-profile-modal">
        <div class="edit-profile-header">
            <h5>编辑资料</h5>
            <button class="edit-profile-close" onclick="closeEditProfileModal()">&#10005;</button>
        </div>
        <div class="edit-profile-body">
            <!-- 头像 -->
            <div class="edit-profile-field">
                <div class="edit-profile-label">头像</div>
                <div class="edit-avatar-area">
                    <img id="editAvatarPreview"
                         th:src="${session.currentUser.avatar != null ? session.currentUser.avatar : '/images/default-avatar.png'}"
                         class="edit-avatar-preview" alt="头像">
                    <div>
                        <input type="file" id="editAvatarFile" accept="image/*" style="display: none;" onchange="initEditCrop(this.files[0])">
                        <button type="button" class="edit-avatar-upload-btn" onclick="document.getElementById('editAvatarFile').click()">
                            <i class="bi bi-upload"></i> 上传新头像
                        </button>
                        <div id="editAvatarMsg" style="font-size:0.72rem;color:rgba(255,255,255,0.35);margin-top:4px;"></div>
                    </div>
                </div>
            </div>
            <!-- 昵称 -->
            <div class="edit-profile-field">
                <div class="edit-profile-label">昵称</div>
                <input type="text" class="edit-profile-input" id="editNickname" th:value="${session.currentUser.nickname}" maxlength="32" placeholder="输入昵称">
            </div>
            <!-- 简介 -->
            <div class="edit-profile-field">
                <div class="edit-profile-label">简介</div>
                <input type="text" class="edit-profile-input" id="editIntro" th:value="${session.currentUser.intro}" maxlength="255" placeholder="介绍一下自己">
            </div>
            <!-- 邮箱 -->
            <div class="edit-profile-field">
                <div class="edit-profile-label">邮箱</div>
                <input type="email" class="edit-profile-input" id="editEmail" th:value="${session.currentUser.email}" maxlength="255" placeholder="输入邮箱">
            </div>
            <!-- 性别 -->
            <div class="edit-profile-field">
                <div class="edit-profile-label">性别</div>
                <select class="edit-profile-select" id="editGender">
                    <option value="0" th:selected="${session.currentUser.gender == null or session.currentUser.gender == 0}">未设置</option>
                    <option value="1" th:selected="${session.currentUser.gender == 1}">男</option>
                    <option value="2" th:selected="${session.currentUser.gender == 2}">女</option>
                </select>
            </div>
            <!-- GitHub -->
            <div class="edit-profile-field">
                <div class="edit-profile-label">GitHub 用户名</div>
                <input type="text" class="edit-profile-input" id="editGithub" th:value="${session.currentUser.githubUsername}" maxlength="255" placeholder="输入 GitHub 用户名">
            </div>
            <button class="edit-profile-save-btn" onclick="saveEditProfile()">保存</button>
        </div>
    </div>
</div>

<!-- 头像裁剪弹窗 -->
<div class="crop-overlay" id="editCropOverlay">
    <div class="crop-modal">
        <div class="crop-header">
            <span>裁剪头像</span>
            <button type="button" class="crop-close" onclick="closeEditCrop()">&#10005;</button>
        </div>
        <div class="crop-body">
            <div class="crop-area">
                <img id="editCropImage" draggable="false">
            </div>
            <div class="crop-preview-side">
                <div class="preview-label">预览</div>
                <div class="preview-circle" id="editPreviewCircle"></div>
                <div style="font-size:0.68rem;color:rgba(255,255,255,0.3);">80 × 80</div>
            </div>
        </div>
        <div class="crop-footer">
            <div class="crop-tools">
                <button type="button" onclick="editCropperRotate(-45)"><i class="bi bi-arrow-counterclockwise"></i></button>
                <button type="button" onclick="editCropperZoom(0.1)"><i class="bi bi-zoom-in"></i></button>
                <button type="button" onclick="editCropperZoom(-0.1)"><i class="bi bi-zoom-out"></i></button>
                <button type="button" onclick="editCropperRotate(45)"><i class="bi bi-arrow-clockwise"></i></button>
                <button type="button" onclick="editCropperReset()"><i class="bi bi-arrow-repeat"></i></button>
            </div>
            <div class="crop-actions">
                <button type="button" class="crop-btn crop-btn-cancel" onclick="closeEditCrop()">取消</button>
                <button type="button" class="crop-btn crop-btn-confirm" onclick="confirmEditCrop()">确认上传</button>
            </div>
        </div>
    </div>
</div>
```

- [ ] **Step 3: Add cropper.js CDN and edit profile JS before </th:block>**

Before `</th:block>` (which closes the fragment), add cropper.js CDN:
```html
<script src="https://cdn.jsdelivr.net/npm/cropperjs@1.6.2/dist/cropper.min.js"></script>
```

And add the edit profile JS within an existing `<script>` block, or add a new one. Place before the closing `</th:block>`:

```html
<script>
    // ===== 编辑资料模态框 =====
    function openEditProfileModal() {
        document.getElementById('editProfileOverlay').classList.add('active');
    }
    function closeEditProfileModal() {
        document.getElementById('editProfileOverlay').classList.remove('active');
    }

    function saveEditProfile() {
        var btn = document.querySelector('.edit-profile-save-btn');
        btn.disabled = true;
        btn.textContent = '保存中...';

        var formData = new URLSearchParams();
        formData.append('nickname', document.getElementById('editNickname').value.trim());
        formData.append('intro', document.getElementById('editIntro').value.trim());
        formData.append('email', document.getElementById('editEmail').value.trim());
        formData.append('gender', document.getElementById('editGender').value);
        formData.append('githubUsername', document.getElementById('editGithub').value.trim());

        fetch('/user/profile/update', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        })
        .then(function(res) { return res.json(); })
        .then(function(data) {
            if (data.code === 200) {
                closeEditProfileModal();
                location.reload();
            } else {
                alert(data.msg || '保存失败');
            }
        })
        .catch(function() { alert('网络错误'); })
        .finally(function() {
            btn.disabled = false;
            btn.textContent = '保存';
        });
    }

    // ===== 头像裁剪 =====
    var editCropper = null;
    var editCropFileName = 'avatar.jpg';

    function initEditCrop(file) {
        if (!file) return;
        if (file.size > 10 * 1024 * 1024) {
            document.getElementById('editAvatarMsg').textContent = '图片不能超过10MB';
            document.getElementById('editAvatarMsg').style.color = '#ff6b6b';
            document.getElementById('editAvatarFile').value = '';
            return;
        }
        editCropFileName = file.name;
        var url = URL.createObjectURL(file);
        document.getElementById('editCropImage').src = url;
        document.getElementById('editCropOverlay').classList.add('active');
        if (editCropper) editCropper.destroy();
        editCropper = new Cropper(document.getElementById('editCropImage'), {
            aspectRatio: 1,
            viewMode: 1,
            dragMode: 'move',
            autoCropArea: 1,
            preview: '#editPreviewCircle',
            background: false,
            rotatable: true,
            scalable: true,
            zoomable: true,
            zoomOnWheel: true,
            cropBoxMovable: true,
            cropBoxResizable: true,
            toggleDragModeOnDblclick: false
        });
    }

    function editCropperRotate(deg) { if (editCropper) editCropper.rotate(deg); }
    function editCropperZoom(delta) { if (editCropper) editCropper.zoom(delta); }
    function editCropperReset() { if (editCropper) editCropper.reset(); }

    function closeEditCrop() {
        document.getElementById('editCropOverlay').classList.remove('active');
        document.getElementById('editAvatarFile').value = '';
        if (editCropper) { editCropper.destroy(); editCropper = null; }
        var img = document.getElementById('editCropImage');
        if (img.src && img.src.startsWith('blob:')) { URL.revokeObjectURL(img.src); img.src = ''; }
    }

    function confirmEditCrop() {
        if (!editCropper) return;
        var canvas = editCropper.getCroppedCanvas({ width: 256, height: 256, imageSmoothingQuality: 'high' });
        if (!canvas) return;
        canvas.toBlob(function(blob) {
            if (!blob) return;
            var msgEl = document.getElementById('editAvatarMsg');
            msgEl.textContent = '上传中...';
            msgEl.style.color = 'rgba(255,255,255,0.5)';
            var formData = new FormData();
            formData.append('file', blob, editCropFileName);
            fetch('/user/avatar/upload', { method: 'POST', body: formData })
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data.code === 200) {
                    document.getElementById('editAvatarPreview').src = data.data + '?t=' + Date.now();
                    msgEl.textContent = '头像已更新';
                    msgEl.style.color = '#6bd9a0';
                    closeEditCrop();
                } else {
                    msgEl.textContent = data.msg || '上传失败';
                    msgEl.style.color = '#ff6b6b';
                    closeEditCrop();
                }
            })
            .catch(function() {
                msgEl.textContent = '上传失败';
                msgEl.style.color = '#ff6b6b';
                closeEditCrop();
            });
        }, 'image/jpeg', 0.85);
    }
</script>
```

- [ ] **Step 4: Remove the old settings link from dropdown**

Already done in step 1 above (removed `<a th:href="@{/user/settings}" ...>`).

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/templates/fragment/header.html
git commit -m "feat: add edit profile modal to header, remove settings link"
```

---

### Task 7: Remove settings.html

**Files:**
- Delete: `src/main/resources/templates/settings.html`

- [ ] **Step 1: Delete settings.html**

```bash
rm src/main/resources/templates/settings.html
```

- [ ] **Step 2: Commit**

```bash
git rm src/main/resources/templates/settings.html
git commit -m "chore: remove settings page, replaced by edit profile modal"
```

---

### Task 8: Build and verify

- [ ] **Step 1: Build project**

```bash
mvn clean package -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 2: Start the app and smoke test**

```bash
mvn spring-boot:run
```
Verify: http://localhost:8080/user/profile loads without errors, shows the new layout.

- [ ] **Step 3: Verify key behaviors**

- 登录后访问 `/user/profile`，个人信息卡片左对齐，LV 徽章显示
- 头像下方显示 UID: xxx
- 经验条正确显示
- 搜索 + 排序按钮切换文章列表
- 分页正常
- 最近点赞区块可见（自己主页）
- 访问他人主页（`?id=其他用户ID`）不显示点赞区块，但显示关注按钮
- 顶栏头像下拉 → 编辑资料 → 模态框弹出 → 可编辑并保存
- GitHub 区块正常展示
