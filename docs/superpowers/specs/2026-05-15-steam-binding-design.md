# Steam 用户绑定与资料展示设计

## 目标

允许博客用户绑定自己的 Steam 账号（通过 SteamID64 或自定义 URL 路径名），在个人主页以自定义风格展示基础资料、游戏库和成就进度。

## 架构

```
用户输入(SteamID64 / URL / 路径名)
     │
     ▼
POST /user/steam/bind  ──▶ SteamService.resolveSteamId()
     │                         │
     │                         ├── 纯数字 → 直接用作 SteamID64
     │                         └── 字符串 → SteamApiClient.resolveVanityURL()
     │
     ▼
更新 users.steam_id ──▶ MySQL
     │
     ▼
GET /api/steam/profile?userId=xxx
     │
     ▼
SteamService.getPlayerProfile()
     │
     ├── GetPlayerSummaries  ──▶ 基础资料（头像、昵称、等级、注册时间、在线状态）
     ├── GetOwnedGames       ──▶ 游戏库（最近游玩、游戏时长），按 playtime 排序取前 20
     └── GetPlayerAchievements ─▶ 每个游戏成就（逐一获取，最多取前 5 个游戏）
     │
     ▼
SteamProfileVO ──▶ JSON 响应 ──▶ 前端渲染
```

- 无 Redis 缓存（Steam API 有内置 CDN 缓存，玩家数据不频繁变化）
- 解绑端点 `DELETE /user/steam/unbind` 清空 `users.steam_id`

## Steam Web API 接口使用

| API 端点 | 用途 | 参数 |
|----------|------|------|
| `ISteamUser/ResolveVanityURL/v0001` | 自定义 URL → SteamID64 | `vanityurl` |
| `ISteamUser/GetPlayerSummaries/v0002` | 玩家基础资料 | `steamids` |
| `IPlayerService/GetOwnedGames/v0001` | 拥有游戏 + 游玩时长 | `steamid`, `include_appinfo=true`, `include_played_free_games=true` |
| `ISteamUserStats/GetPlayerAchievements/v0001` | 某游戏的成就进度 | `steamid`, `appid` |

Steam API 基础 URL：`https://api.steampowered.com`，所有请求附带 `?key={STEAM_API_KEY}`。

## 数据模型

### 数据库变更

```sql
ALTER TABLE users ADD COLUMN steam_id VARCHAR(20) NULL COMMENT 'SteamID64';
```

### Users 实体追加字段

```java
@Size(max = 20)
String steamId;  // SteamID64，空表示未绑定
```

### SteamProfileVO（前端展示用）

```java
public class SteamProfileVO {
    String steamId;           // SteamID64
    String personaName;       // 昵称
    String avatarFull;        // 大头像
    String profileUrl;        // Steam 社区资料页链接
    Integer personaState;     // 在线状态码：0离线 1在线 2忙碌...
    Long timeCreated;         // 注册时间（unix timestamp）
    Integer playerLevel;      // 等级（需从 GetSteamLevel 获取，暂不加，留注释说明）
    List<GameVO> topGames;    // 按 playtime 排序的游戏（前 20）
    List<GameAchievementVO> achievements; // 前 5 个游戏的成就
}

public class GameVO {
    Integer appId;
    String name;              // 游戏名
    Integer playtimeForever;  // 总游玩分钟数
    String imgIconUrl;        // 游戏图标
}

public class GameAchievementVO {
    Integer appId;
    String gameName;
    Integer achieved;         // 已解锁
    Integer total;            // 总数
    double percentage;        // 解锁比例
}
```

## 前端设计

### profile.html 新增区块

- 参考现有 GitHub 区块卡片风格
- 未绑定时展示引导文案："未绑定 Steam 账号" + 跳转编辑资料提示
- 已绑定展示三块：
  1. **资料卡**：Steam 头像 + 昵称 + 在线状态指示点 + 注册时间
  2. **游戏库**：横向滚动卡片，每张显示游戏图标 + 名称 + 游玩时长
  3. **成就**：成就进度条列表（游戏名 + 已解锁/总数 + 百分比）

### 编辑资料弹窗新增字段

- "Steam 绑定" 输入框（placeholder: "SteamID64 或 /id/ 自定义 URL 路径"）
- 解绑按钮（已绑定时显示）
- 后端 `POST /user/profile/update` 追加 `steamId` 参数

### steam.js

- 函数 `loadSteamProfile(steamId)` — 获取并渲染 Steam 数据

## Steam API Key 配置

### application.yml

```yaml
steam:
  api-key: ${STEAM_API_KEY:}
```

### SteamConfig 配置类

```java
@ConfigurationProperties("steam")
public record SteamConfig(String apiKey) {}
```

### SteamApiClient

参照 `SeniverseApiClient` 模式：
`RestTemplate` + `UriComponentsBuilder` 构建 URL，调用后解析 JSON，返回 `SteamProfileVO`。

## 边界处理

| 场景 | 处理 |
|------|------|
| Steam API Key 未配置 | 启动不阻塞，调用时返回明确错误信息"Steam API 未配置" |
| API 调用超时/失败 | 返回空数据 + 前端展示"Steam 数据获取失败，请稍后重试" |
| 自定义 URL 不存在 | `ResolveVanityURL` 返回 success=42，提示"未找到该 Steam 用户" |
| 用户资料为私密 | `GetPlayerSummaries` 返回有限字段，游戏库为空是正常现象 |
| 游戏数为 0 | 展示"该用户游戏库为空或设为私密" |
| 成就不可用 | 跳过该游戏，展示已成功获取的成就 |

## 不在此次范围内的功能

- Steam 登录/OAuth 认证（仅做资料绑定，不做登录）
- 好友列表、库存物品（C 选项选的是基础资料 + 游戏库 + 成就）
- Steam 等级获取（需额外 API `IPlayerService/GetSteamLevel`，后续可加）
- Redis 缓存（Steam 数据非高频访问，API 本身有 CDN 缓存）
- 批量用户 Steam 数据展示（仅限个人主页）
