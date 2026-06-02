# EasySurvivor — Web Edition

## 概述

EasySurvivor 是一款俯视角弹幕生存小游戏，玩家操控角色在敌人包围中存活并获取高分。

**原版**: C++ / EasyX (GDI+ 图形库)，Windows 桌面原生程序  
**Web 版**: JavaScript / HTML5 Canvas，集成于 Spring Boot 3 + Thymeleaf 博客系统

## 改造要点

| 原版 (C++/EasyX) | Web 版 (JS/Canvas) |
|---|---|
| EasyX `putimage` + `AlphaBlend` 透明度合成 | Canvas `drawImage` 原生 PNG 透明度 |
| `GetTickCount` + `Sleep` 帧率控制 | `requestAnimationFrame` + delta-time 标准化 |
| C++ Button / Menu 类 (三态贴图) | HTML + CSS 覆盖层 (menu / gameover) |
| `MessageBox` 显示游戏结束 | CSS 覆盖层面板，含得分/等级/按钮 |
| 手动管理图片帧数组 | `Animation` 类 (路径模板 + 帧索引) |
| MSVC 编译器 `/utf-8` 解决中文乱码 | 天然 UTF-8，无需编码处理 |
| `SOLIDTRANS` 半透明遮罩 | CSS `rgba()` 背景 |

## 功能

- **菜单系统**: 开始游戏 / 返回首页，背景图 + CSS 半透明遮罩
- **操作**: WASD / 方向键移动，主角带行走动画
- **等级系统**: 5 级 (阈值 10 / 25 / 40 / 60 分)，升级增加环绕子弹数
- **生命值**: 3 条命起，Lv2 & Lv4 各 +1 命，受伤后 1.2 秒无敌 (闪烁)
- **道具掉落**: 敌人 30% 概率掉落 6 种道具 (加命 / 无敌 / 弹速↑ / 移速↑ / 全灭 / 冻结)
- **HUD**: 得分 / 等级 / 心形生命图标 / EXP 进度条 / Buff 剩余时间
- **升级提示**: 中央浮动淡出文字

## 文件结构

```
games/easysurvivor/
├── easysurvivor.js   # 游戏引擎 (~650 行)
├── game.css          # 覆盖层样式 + 按钮
├── gameReadMe.md     # 本文件
├── img/              # 精灵帧 PNG (91 张)
│   ├── background.png
│   ├── menu.png
│   ├── shadow_player.png / shadow_enemy.png
│   ├── paimon_left_%d.png / paimon_right_%d.png  (角色动画)
│   └── boar_left_%d.png / boar_right_%d.png      (敌人动画)
└── mus/
    ├── bgm.mp3       # 背景音乐 (循环)
    ├── hit.wav       # 命中音效
    └── hurt.wav      # 受伤音效
```

## 入口

- 博客导航栏 "小游戏" → `/game` → `IndexController.game()` → `easysurvivor.html`
- 画布固定 1280×720，max-width 100% 响应缩放
