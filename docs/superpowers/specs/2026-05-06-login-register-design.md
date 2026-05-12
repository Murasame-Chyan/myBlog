# 登录注册机制 — 设计说明

**日期**: 2026-05-06
**状态**: 已确认

## 概述

为博客增加邮箱+密码的登录注册功能，采用模态弹窗交互，Session 认证方式，BCrypt 密码加密。

## 交互设计

- **入口**：导航栏中央头像。未登录时点击弹出登录/注册模态窗；已登录时显示用户菜单（暂不实现下拉菜单）
- **模态窗**：玻璃拟态风格，暗底 + Sky Blue 点缀，Tab 切换登录/注册
- **登录表单**：邮箱 + 密码，天蓝渐变按钮
- **注册表单**：昵称 + 邮箱 + 密码，紫粉渐变按钮（与"发布"按钮一致）
- **关闭方式**：点击右上角 ✕ 或点击遮罩层
- **提交方式**：AJAX，不刷新页面，成功则 reload

## 数据库变更

`users` 表新增 `password` 字段（VARCHAR(255)），存储 BCrypt 哈希值：

```sql
ALTER TABLE users ADD COLUMN password VARCHAR(255);
```

## 后端设计

### Entity: Users.java
- 新增 `password` 字段（不序列化到 JSON）

### Mapper: UserMapper.java
- `getUserByEmail(email)` — `@Select("SELECT * FROM users WHERE email=#{email}")`
- `insertUser(user)` — `@Insert` (nickname, email, password)，使用 `@Options(useGeneratedKeys=true)` 回填 id

### Service: UserService / UserServiceImpl
- `register(email, nickname, password)` — 查重 → BCrypt 加密 → 插入 → 返回 Users
- `login(email, password)` — 查 email → BCrypt 验密 → 返回 Users 或 null

### Controller: AuthController.java (新建)
- `POST /auth/login` — 参数 email, password。成功写 `session.setAttribute("currentUser", user)`，返回 200
- `POST /auth/register` — 参数 email, nickname, password。成功写 session，返回 200
- `POST /auth/logout` — `session.invalidate()`，返回 200
- `GET /auth/status` — 返回 `{"loggedIn": true/false, "nickname": "...", "avatar": "..."}`

全部返回 JSON（`@ResponseBody`），统一用 `ReturnUtil` 封装。

### Config: SecurityConfiguration.java
- 注入 `BCryptPasswordEncoder` Bean

## 前端设计

### header.html
- 未登录：头像位置包裹按钮，`onclick` 触发打开模态窗
- 已登录：现有逻辑不变（显示 `session.currentUser.avatar`）
- 模态窗 HTML 放在 header fragment 底部，用 `display:none` 默认隐藏

### CSS: common.css
- 新增 模态遮罩、容器、Tab、输入框、按钮 全套样式
- 色调：Sky Blue `#87CEEB` + Orchid Pink `#DA70D6→#FF69B4`
- 风格：暗底半透明 + backdrop-blur 玻璃拟态

### JS: auth.js (新建)
- `openAuthModal()` / `closeAuthModal()` — 显示/隐藏模态窗
- `switchAuthTab(tab)` — 切换登录/注册面板
- `handleLogin()` / `handleRegister()` — AJAX POST，处理响应
- 表单前端校验（邮箱格式、密码长度 >= 6、昵称非空、确认密码一致）

## 安全要点

- 密码 BCrypt 加密，不存明文
- Session Cookie 为 HttpOnly，JS 不可读取
- `/auth/**` 禁用 CSRF（AJAX + 公开端点，风险评估可接受）
- 注册时服务端校验邮箱唯一性

## 文件清单

| 文件 | 操作 |
|------|------|
| `entity/Users.java` | 改 — 加 password 字段 |
| `mapper/UserMapper.java` | 改 — 加 2 个方法 |
| `service/UserService.java` | 改 — 加 2 个接口 |
| `service/impl/UserServiceImpl.java` | 改 — 加 2 个实现 |
| `config/SecurityConfiguration.java` | 改 — 加 BCrypt Bean |
| `controller/AuthController.java` | 新建 |
| `templates/fragment/header.html` | 改 — 嵌入模态窗 HTML + 入口逻辑 |
| `static/css/common.css` | 改 — 模态窗样式 |
| `static/js/auth.js` | 新建 |
