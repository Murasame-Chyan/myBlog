# 个人主页重构设计

## 概述

重构 `/user/profile` 页面布局，将居中卡片改为左对齐信息卡，增加等级/经验展示、文章列表（搜索+排序+分页）、最近点赞展示。编辑资料改为顶栏头像下拉触发模态框，移除 `/user/settings` 独立页面。个人主页支持查看他人（`?id=` 参数）。

---

## 1. 路由变更

| 路由 | 变更 |
|------|------|
| `/user/profile` | 支持可选 `?id=` 参数，不传则展示当前登录用户 |
| `/user/profile?id=123` | 展示用户 123 的主页 |
| `/user/settings` | **移除**，重定向到 `/user/profile` |
| `/user/settings` (POST) | **移除**，逻辑迁移到新 API |
| `/user/profile/update` (POST) | **新增**，编辑资料 API（原 settings POST 逻辑） |

未登录用户访问 `/user/profile` 重定向到首页。

---

## 2. 个人信息卡片

### 布局

三列横向排列，在同一行内：

- **左**：头像（80x80 圆形），头像下方显示 `UID：{id}`
- **中**：昵称 + LV 徽章、性别、经验条（220px 宽）、"距下一级还需 xxx 经验"
- **右**：关注按钮（占位，上下居中）

### 等级系统

Bilibili 式分级阈值表：

```java
private static final long[] LEVEL_THRESHOLDS = {
    0, 200, 1500, 4500, 10800, 28800, 65000, 140000, 300000, 600000
};
```

`calculateLevel(exp)` 遍历阈值表返回最大满足的等级。当前系统暂不涉及经验值增减，直接读 DB 的 `level` 字段展示。

### LV 徽章

昵称右侧紧跟一个渐变粉色小徽章 `LV.{level}`，样式参考 B 站。

### 经验条

220px 宽度，6px 高度，圆角。最大值固定 100000（或下一级阈值）。当前经验值占进度条色块。色块颜色 `linear-gradient(90deg, #87CEEB, #6BB5D9)`。

---

## 3. 编辑资料模态框

### 触发方式

仅通过**顶栏头像下拉菜单**中的"编辑资料"项触发。主页头像不出现下拉菜单。

### 模态框内容

复刻当前 `/user/settings` 页面的编辑功能：头像上传（含裁剪）、昵称、简介、邮箱、性别、GitHub 用户名。

### 实现

在 `header.html` 中已有头像下拉菜单，新增"编辑资料"项，点击打开一个隐藏的模态框。模态框内容内联在 header 中或通过 JS 动态加载。提交改为 AJAX，成功后刷新页面。

### 移除

- `/user/settings` GET/POST 路由
- `settings.html` 模板

---

## 4. 我的文章区

### 数据来源

新增 Mapper 方法按 `u_id` 查询博客，支持关键词模糊搜索标题、排序、分页。

### 排序按钮

三个互斥按钮：最新发布（默认）、最多阅读、最多点赞。点击切换排序并重新加载列表。

### 搜索

搜索框输入标题关键词，回车或点击搜索图标触发。与当前排序按钮组合使用。

### 分页

每页 10 条，底部分页导航。

### 后端 API

`GET /user/profile/blogs?userId={id}&keyword={}&sortBy={newest|reads|likes}&page={}&pageSize=10`

返回 `{code, msg, data: {list: [...], total, page, pageSize}}`。

---

## 5. 最近点赞文章

### 可见性

仅当**当前登录用户 ID == 主页用户 ID** 时显示。外部访客不可见。

### 数据来源

`Users.liked_b_id` 字段存储 JSON 数组 `[blogId1, blogId2, ...]`。在 Service 层解析后批量查询博客标题和日期，最多取最近 10 条。

### 后端 API

`GET /user/profile/likes?userId={id}`

只有当前登录用户 ID 与请求 userId 一致时才返回数据，否则返回空。

---

## 6. 关注按钮

占位按钮，点击不做任何操作（或弹出"功能开发中"提示），样式完成。位置在个人信息卡片最右侧，上下居中。

后续可扩展 `follows` 表实现真实关注系统。

---

## 7. 后端改动清单

| 文件 | 改动 |
|------|------|
| `UserController.java` | profile 方法支持 `@RequestParam(required=false) Long id`；新增 `/user/profile/update` POST；新增 `/user/profile/blogs` GET；新增 `/user/profile/likes` GET；移除 settings 路由 |
| `UserService.java` | 新增 `calculateLevel(int exp)`；新增 `getLikedBlogs(Long userId)` |
| `UserServiceImpl.java` | 实现等级计算、点赞文章查询 |
| `BlogMapper.java` | 新增 `getBlogsByUserId(userId, keyword, sortBy, pageSize, offset)` 和对应 count 方法 |
| `BlogService.java` | 新增 `getUserBlogs(...)` 和 `countUserBlogs(...)` |
| `BlogServiceImpl.java` | 实现用户文章查询 |

---

## 8. 前端改动清单

| 文件 | 改动 |
|------|------|
| `profile.html` | 完全重写：左对齐信息卡片 + 文章列表区 + 点赞区 + JS |
| `header.html` | 头像下拉菜单添加"编辑资料"项 + 内联编辑模态框 |
| `common.css` | 新增个人信息卡片样式、LV 徽章、经验条、文章列表、排序按钮、点赞区样式 |
| `settings.html` | **删除** |
| `static/js/` | 新增或修改 profile 相关 JS 处理搜索、排序、分页、模态框 |

---

## 9. 非目标

- 不实现真实关注/取关功能
- 不实现经验值增减逻辑（只读展示）
- 不移除或修改 GitHub 展示区块
- 不修改评论区、归档等无关模块
