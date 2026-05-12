# 登录注册功能 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为博客添加邮箱+密码的登录注册功能，模态弹窗交互，Session 认证，BCrypt 加密。

**Architecture:** 自底向上逐层实现：数据库 → Entity → Mapper → Service → Config → Controller → CSS → JS → Template。后端遵循现有 controller/service/mapper 分层，前端延续 Thymeleaf + Bootstrap + 原生 JS 模式。

**Tech Stack:** Spring Boot 3.5.x, MyBatis, Thymeleaf, BCrypt, Bootstrap 5, 原生 JavaScript

---

### Task 1: 数据库迁移

**Files:**
- Create: `src/main/resources/db/migration.sql` (SQL 参考文件)

- [ ] **Step 1: 执行 ALTER TABLE**

连接到 MySQL `myblog` 数据库，执行：

```sql
ALTER TABLE users ADD COLUMN password VARCHAR(255) DEFAULT NULL;
```

MySQL 命令行：
```bash
mysql -u root -p myblog -e "ALTER TABLE users ADD COLUMN password VARCHAR(255) DEFAULT NULL;"
```

已验证有 `users` 表，字段: `id, nickname, intro, avatar, level, email, gender, exp, liked_b_id`。新增 `password` 存储 BCrypt 哈希。

- [ ] **Step 2: 验证**

```bash
mysql -u root -p myblog -e "DESCRIBE users;"
```

预期：输出中应包含新增的 `password` 列。

---

### Task 2: Users Entity — 添加 password 字段

**Files:**
- Modify: `src/main/java/com/murasame/entity/Users.java`

- [ ] **Step 1: 添加 password 字段**

```java
package com.murasame.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Users {
    Long id;
    String nickname;
    String intro;
    String avatar;
    Integer level;
    String email;
    Integer gender;
    Integer exp;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String liked_b_id;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String password;
}
```

`@JsonProperty(access = WRITE_ONLY)` 确保 password 不会被序列化到 JSON 响应中，但可以从请求体接收。

---

### Task 3: UserMapper — 新增查询和插入方法

**Files:**
- Modify: `src/main/java/com/murasame/mapper/UserMapper.java`

- [ ] **Step 1: 添加 getUserByEmail 和 insertUser**

在现有接口中添加两个方法：

```java
@Select("SELECT * FROM users WHERE email=#{email}")
Users getUserByEmail(@Param("email") String email);

@Insert("INSERT INTO users (nickname, email, password) VALUES (#{nickname}, #{email}, #{password})")
@Options(useGeneratedKeys = true, keyProperty = "id")
int insertUser(Users user);
```

需要新增导入：

```java
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
```

完整文件结构保持现有 `getUserById`, `getNicknameById`, `updateLikedBId`, `updateAvatar` 不变，在末尾追加两个新方法。

---

### Task 4: UserService 接口 — 新增 login 和 register

**Files:**
- Modify: `src/main/java/com/murasame/service/UserService.java`

- [ ] **Step 1: 添加接口方法**

```java
Users register(String email, String nickname, String password);

Users login(String email, String password);
```

完整接口（现有方法保留）：

```java
package com.murasame.service;

import com.murasame.entity.Users;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    Users getUserById(Long id);

    String getNicknameById(Long id);

    int updateLikedBId(Long userId, String likedBId);

    boolean isBlogLiked(Long userId, Long blogId);

    int updateAvatar(Long userId, String avatarUrl);

    Users register(String email, String nickname, String password);

    Users login(String email, String password);
}
```

---

### Task 5: SecurityConfiguration — 添加 BCryptPasswordEncoder Bean

**Files:**
- Modify: `src/main/java/com/murasame/config/SecurityConfiguration.java`

- [ ] **Step 1: 添加 Bean 定义**

在类中添加：

```java
@Bean
public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

新增导入：

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
```

完整类保持不变，仅在现有 `securityFilterChain` Bean 之后添加新 Bean。

---

### Task 6: UserServiceImpl — 实现 login 和 register

**Files:**
- Modify: `src/main/java/com/murasame/service/impl/UserServiceImpl.java`

- [ ] **Step 1: 注入 BCryptPasswordEncoder 并实现 register**

在类中添加：

```java
@Resource
private BCryptPasswordEncoder passwordEncoder;
```

新增导入：

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
```

实现 `register`:

```java
@Override
public Users register(String email, String nickname, String password) {
    if (email == null || email.isBlank()) {
        throw new IllegalArgumentException("邮箱不能为空");
    }
    if (password == null || password.length() < 6) {
        throw new IllegalArgumentException("密码至少6位");
    }
    Users existing = userMapper.getUserByEmail(email);
    if (existing != null) {
        throw new IllegalArgumentException("该邮箱已被注册");
    }
    Users user = new Users();
    user.setNickname(nickname != null && !nickname.isBlank() ? nickname : email.split("@")[0]);
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode(password));
    userMapper.insertUser(user);
    return user;
}
```

- [ ] **Step 2: 实现 login**

```java
@Override
public Users login(String email, String password) {
    if (email == null || email.isBlank() || password == null || password.isBlank()) {
        return null;
    }
    Users user = userMapper.getUserByEmail(email);
    if (user == null || user.getPassword() == null) {
        return null;
    }
    if (!passwordEncoder.matches(password, user.getPassword())) {
        return null;
    }
    return user;
}
```

---

### Task 7: AuthController — 创建认证控制器

**Files:**
- Create: `src/main/java/com/murasame/controller/AuthController.java`

- [ ] **Step 1: 创建控制器类**

```java
package com.murasame.controller;

import com.murasame.entity.Users;
import com.murasame.service.UserService;
import com.murasame.util.ReturnUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("/auth")
@Controller
@Tag(name = "认证接口", description = "登录、注册、登出")
public class AuthController {

    @Resource
    private UserService userService;

    @ResponseBody
    @PostMapping("/login")
    public Map<String, Object> login(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session) {
        Users user = userService.login(email, password);
        if (user == null) {
            return ReturnUtil.error("邮箱或密码错误");
        }
        session.setAttribute("currentUser", user);
        return ReturnUtil.success("登录成功");
    }

    @ResponseBody
    @PostMapping("/register")
    public Map<String, Object> register(
            @RequestParam String email,
            @RequestParam String nickname,
            @RequestParam String password,
            HttpSession session) {
        try {
            Users user = userService.register(email, nickname, password);
            session.setAttribute("currentUser", user);
            return ReturnUtil.success("注册成功");
        } catch (IllegalArgumentException e) {
            return ReturnUtil.error(e.getMessage());
        }
    }

    @ResponseBody
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.invalidate();
        return ReturnUtil.success("已登出");
    }

    @ResponseBody
    @GetMapping("/status")
    public Map<String, Object> status(HttpSession session) {
        Users user = (Users) session.getAttribute("currentUser");
        if (user == null) {
            return ReturnUtil.custom(200, "未登录", Map.of("loggedIn", false));
        }
        return ReturnUtil.custom(200, "已登录", Map.of(
                "loggedIn", true,
                "nickname", user.getNickname() != null ? user.getNickname() : "",
                "avatar", user.getAvatar() != null ? user.getAvatar() : ""
        ));
    }
}
```

---

### Task 8: common.css — 添加模态窗样式

**Files:**
- Modify: `src/main/resources/static/css/common.css`

- [ ] **Step 1: 在文件末尾追加模态窗样式**

```css
/* ===== 登录/注册模态窗 ===== */
.auth-overlay {
    display: none;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.55);
    backdrop-filter: blur(4px);
    -webkit-backdrop-filter: blur(4px);
    z-index: 1050;
    align-items: center;
    justify-content: center;
}
.auth-overlay.show {
    display: flex;
}

.auth-modal {
    background: linear-gradient(
        160deg,
        rgba(255, 255, 255, 0.12) 0%,
        rgba(255, 255, 255, 0.06) 100%
    );
    backdrop-filter: blur(24px);
    -webkit-backdrop-filter: blur(24px);
    border-radius: 20px;
    border: 1px solid rgba(135, 206, 235, 0.2);
    box-shadow:
        0 8px 32px rgba(0, 0, 0, 0.4),
        0 2px 8px rgba(135, 206, 235, 0.08),
        inset 0 1px 0 rgba(255, 255, 255, 0.08);
    width: 420px;
    max-width: 90vw;
    overflow: hidden;
    animation: authModalIn 0.25s ease;
}
@keyframes authModalIn {
    from { opacity: 0; transform: translateY(-20px) scale(0.96); }
    to   { opacity: 1; transform: translateY(0) scale(1); }
}

.auth-modal-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 18px 24px;
    border-bottom: 1px solid rgba(135, 206, 235, 0.12);
}
.auth-modal-logo {
    font-weight: 700;
    font-size: 1.15rem;
    color: #fff;
    letter-spacing: 0.5px;
}
.auth-modal-logo span { color: #87CEEB; }
.auth-modal-close {
    width: 30px; height: 30px;
    border-radius: 50%;
    border: 1px solid rgba(255, 255, 255, 0.15);
    background: rgba(255, 255, 255, 0.06);
    color: rgba(255, 255, 255, 0.5);
    cursor: pointer;
    display: flex; align-items: center; justify-content: center;
    font-size: 0.9rem;
    transition: all 0.25s;
    line-height: 1;
}
.auth-modal-close:hover {
    color: #fff;
    background: rgba(255, 255, 255, 0.12);
    border-color: rgba(255, 255, 255, 0.3);
}

.auth-modal-tabs {
    display: flex;
    background: rgba(135, 206, 235, 0.05);
    border-bottom: 1px solid rgba(135, 206, 235, 0.1);
}
.auth-tab {
    flex: 1;
    text-align: center;
    padding: 14px 0;
    cursor: pointer;
    font-weight: 600;
    font-size: 0.9rem;
    color: rgba(255, 255, 255, 0.45);
    border-bottom: 2px solid transparent;
    transition: all 0.3s;
    letter-spacing: 0.3px;
}
.auth-tab:hover {
    color: rgba(255, 255, 255, 0.7);
    background: rgba(135, 206, 235, 0.05);
}
.auth-tab.active {
    color: #87CEEB;
    border-bottom-color: #87CEEB;
    background: rgba(135, 206, 235, 0.08);
}

.auth-modal-body {
    padding: 28px 28px 32px;
}

.auth-form-panel {
    display: none;
}
.auth-form-panel.active {
    display: block;
    animation: authFormIn 0.25s ease;
}
@keyframes authFormIn {
    from { opacity: 0; transform: translateX(10px); }
    to   { opacity: 1; transform: translateX(0); }
}

.auth-form-group {
    margin-bottom: 18px;
}
.auth-form-group label {
    display: block;
    font-size: 0.8rem;
    font-weight: 600;
    color: rgba(255, 255, 255, 0.6);
    margin-bottom: 6px;
    letter-spacing: 0.4px;
}
.auth-form-input {
    width: 100%;
    padding: 11px 14px;
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid rgba(135, 206, 235, 0.18);
    border-radius: 10px;
    font-size: 0.9rem;
    color: #fff;
    outline: none;
    transition: all 0.25s;
    box-sizing: border-box;
}
.auth-form-input::placeholder {
    color: rgba(255, 255, 255, 0.25);
}
.auth-form-input:focus {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(135, 206, 235, 0.5);
    box-shadow: 0 0 0 3px rgba(135, 206, 235, 0.08);
}

.auth-error-msg {
    color: #ff6b6b;
    font-size: 0.78rem;
    margin-top: 4px;
    display: none;
}

.auth-btn-submit {
    width: 100%;
    padding: 12px;
    border: none;
    border-radius: 25px;
    font-size: 0.95rem;
    font-weight: 700;
    cursor: pointer;
    transition: all 0.3s;
    color: #fff;
    letter-spacing: 0.5px;
    margin-top: 6px;
}
.auth-btn-login {
    background: linear-gradient(135deg, #87CEEB 0%, #6BB5D9 100%);
    box-shadow: 0 4px 15px rgba(135, 206, 235, 0.3);
}
.auth-btn-login:hover {
    box-shadow: 0 6px 22px rgba(135, 206, 235, 0.45);
    transform: translateY(-2px);
}
.auth-btn-register {
    background: linear-gradient(135deg, #DA70D6 0%, #FF69B4 100%);
    box-shadow: 0 4px 15px rgba(218, 112, 214, 0.35);
}
.auth-btn-register:hover {
    box-shadow: 0 6px 22px rgba(218, 112, 214, 0.5);
    transform: translateY(-2px);
}

.auth-switch-text {
    text-align: center;
    margin-top: 20px;
    font-size: 0.82rem;
    color: rgba(255, 255, 255, 0.4);
}
.auth-switch-text a {
    color: #87CEEB;
    text-decoration: none;
    font-weight: 600;
    transition: all 0.2s;
    cursor: pointer;
}
.auth-switch-text a:hover {
    color: #DA70D6;
}
```

---

### Task 9: auth.js — 创建前端认证脚本

**Files:**
- Create: `src/main/resources/static/js/auth.js`

- [ ] **Step 1: 编写完整的 auth.js**

```javascript
// ===== 认证模态窗交互 =====

function openAuthModal(tab) {
    var overlay = document.getElementById('authOverlay');
    if (overlay) {
        overlay.classList.add('show');
        switchAuthTab(tab || 'login');
    }
}

function closeAuthModal() {
    var overlay = document.getElementById('authOverlay');
    if (overlay) {
        overlay.classList.remove('show');
    }
    clearAuthErrors();
    clearAuthForms();
}

function clearAuthErrors() {
    document.querySelectorAll('.auth-error-msg').forEach(function(el) {
        el.style.display = 'none';
        el.textContent = '';
    });
}

function clearAuthForms() {
    document.querySelectorAll('.auth-form-input').forEach(function(el) {
        el.value = '';
    });
}

function showAuthError(panel, fieldId, message) {
    var panelEl = document.getElementById(panel);
    if (!panelEl) return;
    var errorEl = panelEl.querySelector('.auth-error-msg[data-field="' + fieldId + '"]');
    if (errorEl) {
        errorEl.textContent = message;
        errorEl.style.display = 'block';
    }
}

function switchAuthTab(tab) {
    var loginTab = document.getElementById('authTabLogin');
    var registerTab = document.getElementById('authTabRegister');
    var loginPanel = document.getElementById('loginPanel');
    var registerPanel = document.getElementById('registerPanel');

    if (tab === 'login') {
        loginTab.classList.add('active');
        registerTab.classList.remove('active');
        loginPanel.classList.add('active');
        registerPanel.classList.remove('active');
    } else {
        registerTab.classList.add('active');
        loginTab.classList.remove('active');
        registerPanel.classList.add('active');
        loginPanel.classList.remove('active');
    }
    clearAuthErrors();
}

function handleLogin() {
    clearAuthErrors();
    var email = document.getElementById('loginEmail').value.trim();
    var password = document.getElementById('loginPassword').value;

    if (!email) {
        showAuthError('loginPanel', 'loginEmail', '请输入邮箱');
        return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        showAuthError('loginPanel', 'loginEmail', '邮箱格式不正确');
        return;
    }
    if (!password) {
        showAuthError('loginPanel', 'loginPassword', '请输入密码');
        return;
    }

    var formData = new URLSearchParams();
    formData.append('email', email);
    formData.append('password', password);

    fetch('/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: formData.toString()
    })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200) {
            location.reload();
        } else {
            showAuthError('loginPanel', 'loginPassword', data.msg || '登录失败');
        }
    })
    .catch(function() {
        showAuthError('loginPanel', 'loginPassword', '网络错误，请稍后重试');
    });
}

function handleRegister() {
    clearAuthErrors();
    var nickname = document.getElementById('registerNickname').value.trim();
    var email = document.getElementById('registerEmail').value.trim();
    var password = document.getElementById('registerPassword').value;
    var confirmPassword = document.getElementById('registerConfirmPassword').value;

    var hasError = false;
    if (!nickname) {
        showAuthError('registerPanel', 'registerNickname', '请输入昵称');
        hasError = true;
    }
    if (!email) {
        showAuthError('registerPanel', 'registerEmail', '请输入邮箱');
        hasError = true;
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        showAuthError('registerPanel', 'registerEmail', '邮箱格式不正确');
        hasError = true;
    }
    if (!password) {
        showAuthError('registerPanel', 'registerPassword', '请输入密码');
        hasError = true;
    } else if (password.length < 6) {
        showAuthError('registerPanel', 'registerPassword', '密码至少6位');
        hasError = true;
    }
    if (password !== confirmPassword) {
        showAuthError('registerPanel', 'registerConfirmPassword', '两次密码不一致');
        hasError = true;
    }
    if (hasError) return;

    var formData = new URLSearchParams();
    formData.append('email', email);
    formData.append('nickname', nickname);
    formData.append('password', password);

    fetch('/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: formData.toString()
    })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200) {
            location.reload();
        } else {
            showAuthError('registerPanel', 'registerEmail', data.msg || '注册失败');
        }
    })
    .catch(function() {
        showAuthError('registerPanel', 'registerEmail', '网络错误，请稍后重试');
    });
}

// 点击遮罩层关闭
document.addEventListener('click', function(e) {
    if (e.target.id === 'authOverlay') {
        closeAuthModal();
    }
});

// 按 ESC 关闭
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        var overlay = document.getElementById('authOverlay');
        if (overlay && overlay.classList.contains('show')) {
            closeAuthModal();
        }
    }
});
```

---

### Task 10: header.html — 嵌入模态窗 + 修改入口

**Files:**
- Modify: `src/main/resources/templates/fragment/header.html`

- [ ] **Step 1: 修改头像区域的点击行为**

将现有头像 div 中的 `onclick` 逻辑替换。找到 `<div class="navbar-avatar mx-auto">` 区域，将其改为：

```html
<!-- 居中头像 -->
<div class="navbar-avatar mx-auto">
  <!-- 已登录：显示用户头像 -->
  <img th:if="${session.currentUser != null and session.currentUser.avatar != null}"
       th:src="${session.currentUser.avatar}"
       alt="用户头像"
       class="rounded-circle avatar-img"
       style="width: 40px; height: 40px; object-fit: cover;">
  <!-- 已登录但无头像：显示默认头像 -->
  <img th:if="${session.currentUser != null and session.currentUser.avatar == null}"
       src="/images/default-avatar.png"
       alt="默认头像"
       class="rounded-circle avatar-img"
       style="width: 40px; height: 40px; object-fit: cover;">
  <!-- 未登录：显示登录入口 -->
  <img th:if="${session.currentUser == null}"
       src="/images/default-avatar.png"
       alt="点击登录"
       class="rounded-circle avatar-img"
       style="width: 40px; height: 40px; object-fit: cover; cursor: pointer;"
       onclick="openAuthModal('login')"
       title="点击登录 / 注册">
</div>
```

- [ ] **Step 2: 在 `</th:block>` 之前，`</nav>` 之后，追加模态窗 HTML 和脚本引用**

```html
<!-- 登录/注册模态窗 -->
<div class="auth-overlay" id="authOverlay">
  <div class="auth-modal">
    <div class="auth-modal-header">
      <div class="auth-modal-logo">My<span>Blog</span></div>
      <button class="auth-modal-close" onclick="closeAuthModal()">&#10005;</button>
    </div>
    <div class="auth-modal-tabs">
      <div class="auth-tab active" id="authTabLogin" onclick="switchAuthTab('login')">登录</div>
      <div class="auth-tab" id="authTabRegister" onclick="switchAuthTab('register')">注册</div>
    </div>
    <div class="auth-modal-body">
      <!-- 登录面板 -->
      <div class="auth-form-panel active" id="loginPanel">
        <div class="auth-form-group">
          <label>邮箱</label>
          <input type="text" class="auth-form-input" id="loginEmail" placeholder="请输入邮箱地址" autocomplete="email">
          <div class="auth-error-msg" data-field="loginEmail"></div>
        </div>
        <div class="auth-form-group">
          <label>密码</label>
          <input type="password" class="auth-form-input" id="loginPassword" placeholder="请输入密码" autocomplete="current-password">
          <div class="auth-error-msg" data-field="loginPassword"></div>
        </div>
        <button class="auth-btn-submit auth-btn-login" onclick="handleLogin()">登 录</button>
        <div class="auth-switch-text">
          还没有账号？<a onclick="switchAuthTab('register')">立即注册</a>
        </div>
      </div>
      <!-- 注册面板 -->
      <div class="auth-form-panel" id="registerPanel">
        <div class="auth-form-group">
          <label>昵称</label>
          <input type="text" class="auth-form-input" id="registerNickname" placeholder="如何称呼你？" autocomplete="nickname">
          <div class="auth-error-msg" data-field="registerNickname"></div>
        </div>
        <div class="auth-form-group">
          <label>邮箱</label>
          <input type="email" class="auth-form-input" id="registerEmail" placeholder="请输入邮箱地址" autocomplete="email">
          <div class="auth-error-msg" data-field="registerEmail"></div>
        </div>
        <div class="auth-form-group">
          <label>密码</label>
          <input type="password" class="auth-form-input" id="registerPassword" placeholder="至少 6 位密码" autocomplete="new-password">
          <div class="auth-error-msg" data-field="registerPassword"></div>
        </div>
        <div class="auth-form-group">
          <label>确认密码</label>
          <input type="password" class="auth-form-input" id="registerConfirmPassword" placeholder="再次输入密码" autocomplete="new-password">
          <div class="auth-error-msg" data-field="registerConfirmPassword"></div>
        </div>
        <button class="auth-btn-submit auth-btn-register" onclick="handleRegister()">注 册</button>
        <div class="auth-switch-text">
          已有账号？<a onclick="switchAuthTab('login')">去登录</a>
        </div>
      </div>
    </div>
  </div>
</div>
<script src="/js/auth.js"></script>
```

---

### Task 11: 集成验证

**Files:** 无（验证步骤）

- [ ] **Step 1: 编译项目**

```bash
mvn clean package -DskipTests
```

预期：BUILD SUCCESS

- [ ] **Step 2: 启动应用**

```bash
mvn spring-boot:run
```

- [ ] **Step 3: 手动验证**

1. 访问 http://localhost:8080，点击头像应弹出模态窗
2. 在注册 Tab 输入昵称、邮箱、密码，点击"注册"
3. 注册成功后页面刷新，导航栏头像应变为默认头像（新用户无自定义头像）
4. 刷新页面，应保持登录状态（session 持久）
5. 点击头像应弹出模态窗，可执行登出

- [ ] **Step 4: 验证错误处理**

1. 注册时用相同邮箱 → 提示"该邮箱已被注册"
2. 登录时用错误密码 → 提示"邮箱或密码错误"
3. 注册时两次密码不一致 → 提示"两次密码不一致"
4. 空字段提交 → 前端校验拦截

---

### Task 12: 提交

- [ ] **Step 1: 提交所有变更**

```bash
git add src/main/java/com/murasame/entity/Users.java
git add src/main/java/com/murasame/mapper/UserMapper.java
git add src/main/java/com/murasame/service/UserService.java
git add src/main/java/com/murasame/service/impl/UserServiceImpl.java
git add src/main/java/com/murasame/config/SecurityConfiguration.java
git add src/main/java/com/murasame/controller/AuthController.java
git add src/main/resources/static/css/common.css
git add src/main/resources/static/js/auth.js
git add src/main/resources/templates/fragment/header.html
git commit -m "feat: add login/register with modal, session auth, BCrypt"
```
