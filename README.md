# Pixel Shooter — 像素飞机大战

一款基于 Android 原生开发的竖版像素风射击游戏。

## 游戏特色

- 🎮 **5个独立关卡**，每关 Boss 各有特色攻击方式
- ✈️ **3架飞机**可选：猎鹰号（穿透）、风暴号（散射）、猎鹰II号（追踪）
- 💥 **丰富道具系统**：血包、炸弹、护盾、强化子弹
- 💾 **关卡解锁 + 存档**：通关解锁下一关，最高分永久保存
- 🎯 **竖屏滑动操控**，手感流畅

## 关卡概览

| 关卡 | 名称 | 特色 |
|------|------|------|
| 第1关 | 蓝天边境 | 新手引导，铁翼长老Boss |
| 第2关 | 火山峡谷 | 自爆飞机，烈焰魔王Boss（有护甲层） |
| 第3关 | 深海上空 | 编队战机，深海主宰Boss（会潜入消失） |
| 第4关 | 雷暴之城 | 护盾战机，雷霆支配者Boss（高速横移） |
| 第5关 | 宇宙终点 | 隐身战机，宇宙毁灭者Boss（三阶段变身）|

## 技术架构

- **语言**：Kotlin
- **渲染**：Android SurfaceView + Canvas（纯代码像素风绘制）
- **架构**：游戏引擎 + 实体组件模式
- **存档**：SharedPreferences + Gson
- **构建**：Gradle 8.4

## 本地构建

```bash
./gradlew assembleDebug
# APK 输出：app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions 自动构建

每次推送到 `main` 分支自动触发构建，APK 作为 Artifact 上传。  
推送 `v*` tag 时自动创建 GitHub Release 并附带 APK。

```bash
# 触发正式发布
git tag v1.0.0
git push origin v1.0.0
```

## 系统要求

- Android 7.0 (API 24) 及以上
- 建议内存 ≥ 2GB

---

*像素飞机大战 v1.0.0 — 移动应用构建师*
