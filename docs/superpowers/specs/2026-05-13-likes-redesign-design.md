# 点赞系统改造设计（Redis + MySQL）

## 问题诊断

1. `users.liked_b_id` JSON 数组：读-改-写竞态、数组越大越慢、无法扩展
2. 所有点赞操作硬编码 `userId=1`，其他用户点赞无效

## 架构

```
用户点赞/取消
     │
     ▼
Controller (session.currentUser)
     │
     ▼
LikesService
     │
     ├── Redis SADD/SREM ─────────────▶ 主存储，O(1)，即时
     │
     └── @Async ──▶ MySQL user_likes ──▶ 持久化备份
                          │
服务启动 ◀── 回灌 ────────┘
```

- **Redis**：热数据，所有读写走 Redis
- **MySQL**：冷备份，异步写入，启动时回灌 Redis
- **读降级**：Redis 命中失败时 fallback MySQL

## Redis 数据结构

```
Key:   user:likes:{userId}           → Set<blogId>
操作:  SADD / SREM / SISMEMBER / SMEMBERS / SCARD
TTL:   无（持久化，启动从 MySQL 回灌）
```

## MySQL 表

```sql
CREATE TABLE user_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    blog_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_blog (user_id, blog_id),
    INDEX idx_user_id (user_id)
);
```

## 后端改动

| 文件 | 改动 |
|------|------|
| `pom.xml` | 添加 `spring-boot-starter-data-redis` |
| `application.yml` | Redis 连接配置 |
| 新增 `config/RedisConfig.java` | RedisTemplate 序列化配置 |
| 新增 `service/LikesService.java` | 接口：like, unlike, isLiked, getLikedBlogs, syncToDb |
| 新增 `service/impl/LikesServiceImpl.java` | Redis 主逻辑 + @Async MySQL 写入 |
| 新增 `mapper/UserLikeMapper.java` | MySQL CRUD |
| 修改 `controller/BlogController.java` | like/unlike/isLiked → 改用 LikesService + session |
| 修改 `controller/UserController.java` | getLikedBlogs → 改用 LikesService |
| 修改 `UserService.java` / `UserServiceImpl.java` | 移除 addBlogToLiked / removeBlogFromLiked / JSON 解析 |
| 修改 `static/js/blogInteraction.js` | 去掉 `userId=1` |

## LikesService 接口

```java
public interface LikesService {
    void like(Long userId, Long blogId);              // Redis SADD + @Async MySQL
    void unlike(Long userId, Long blogId);            // Redis SREM + @Async MySQL
    boolean isLiked(Long userId, Long blogId);        // Redis SISMEMBER, fallback MySQL
    List<BlogBriefVO> getLikedBlogs(Long userId, int limit);  // Redis SMEMBERS → JOIN
    void warmupFromDb();                              // @PostConstruct: MySQL → Redis
}
```

## 数据迁移

启动时自动执行 `warmupFromDb()`：
1. `SELECT user_id, blog_id FROM user_likes`
2. 按 user_id 分组 → `SADD user:likes:{userId} {blogId}`
3. 已有 `liked_b_id` JSON 数据需一次性迁入 `user_likes` 表（SQL 脚本）

迁移后删除 `users.liked_b_id` 列。

## 移除

- `Users.liked_b_id` 字段及 DB 列
- `UserMapper.updateLikedBId`
- `UserServiceImpl` 中 `addBlogToLiked` / `removeBlogFromLiked` / `getLikedBlogIds`

## 非目标

- 不改动 `blogs.like_count`
- 不添加点赞通知
- 不改造前端点赞按钮 UI
