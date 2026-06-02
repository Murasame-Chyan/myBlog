# MyBlog

基于 Spring Boot + Thymeleaf 的深色风格个人博客系统。

已有体验版网站：https://www.murasameblog.top

## 效果预览

### 首页
![首页](docs/screenshots/01-homepage.png)

### 博客详情
![博客详情](docs/screenshots/02-blog-detail.png)

## 技术栈

| 层面 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.5 + Spring MVC |
| 安全框架 | Spring Security + JWT |
| 模板引擎 | Thymeleaf |
| ORM | MyBatis (注解方式) |
| 数据库 | MySQL |
| 缓存 | Redis (认证 token 黑名单) |
| 前端样式 | Bootstrap 5 + 自定义深色毛玻璃主题 |
| Markdown | CommonMark (commonmark-java) |
| 文件存储 | 腾讯云 COS |
| API 文档 | Springdoc OpenAPI |

## 功能

- 博客发布 / 编辑 / 删除 / 回收站恢复
- Markdown 渲染（代码高亮、表格、任务列表、Mermaid 图表）
- 标签系统（MySQL JSON 存储，支持多标签过滤）
- 评论树（支持回复嵌套）
- 用户注册 / 登录 / 密码重置（邮箱验证码）
- JWT 双 token 认证（AccessToken + RefreshToken）
- 个人主页（头像裁剪上传、昵称、简介、GitHub 绑定）
- 文章点赞 / 阅读计数
- 全文搜索 + 时间范围筛选 + 排序
- 热门文章 / 最新评论侧边栏
- 心知天气组件
- 关注 / 粉丝系统
- GitHub 贡献热力图

## 本地运行

```bash
# 1. 配置 application.properties（参考下方说明）
# 2. 启动
mvn spring-boot:run
# 3. 访问
open http://localhost:8080
```

## 配置项

在 `application.properties` 中配置（该文件已被 .gitignore 排除，不会提交）：

```properties
server.port=8080

# 数据库
spring.datasource.url=jdbc:mysql://localhost:3306/myblog
spring.datasource.username=root
spring.datasource.password=你的数据库密码

# Redis（可选，用于 JWT 黑名单）
spring.data.redis.host=localhost
spring.data.redis.port=6379

# 邮箱（用于发送验证码）
spring.mail.host=smtp.qq.com
spring.mail.port=587
spring.mail.username=你的邮箱
spring.mail.password=SMTP授权码

# 心知天气
weather.seniverse.key=你的私钥
weather.seniverse.uid=你的公钥

# 腾讯云 COS（文件上传）
tencent.cos.secret-id=你的SecretId
tencent.cos.secret-key=你的SecretKey
tencent.cos.bucket-name=你的Bucket名
tencent.cos.base-url=你的CDN地址

# JWT
jwt.secret=随机生成的32字符以上密钥
```

## 项目结构

```
src/main/java/com/murasame/
├── controller/    # HTTP 控制器
├── service/       # 业务逻辑层
├── mapper/        # MyBatis 数据访问
├── entity/        # 数据库实体
├── domain/        # DTO / VO
├── config/        # Spring 配置
├── client/        # 外部 API 客户端
└── util/          # 工具类

src/main/resources/
├── templates/     # Thymeleaf 模板
├── static/        # 静态资源 (CSS/JS/images)
└── application*.yml
```
