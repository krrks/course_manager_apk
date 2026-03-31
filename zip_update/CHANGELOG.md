#!build
# 新增功能：GitHub 数据同步

## 功能概述
在设置页面新增「GitHub 数据同步」入口，支持将全部应用数据推送到用户私有 GitHub 仓库，
或从仓库拉取数据到本地，实现多设备共享与云端备份。

## 数据存储位置
数据写入目标仓库的 `userdata_sync/` 目录：
- `userdata_sync/meta.json`    — 同步元信息（时间、版本、数据 hash）
- `userdata_sync/state.json`   — 全量 AppState（复用现有 Gson 序列化）
- `userdata_sync/avatars/`     — 头像图片文件

## 新增文件
- `GitHubSyncService.kt`    — GitHub Contents API 封装（HttpURLConnection，无新依赖）
- `GitHubSyncViewModel.kt`  — 推送/拉取编排、冲突检测、PAT 加密存储
- `GitHubSyncScreen.kt`     — 设置子页面 UI（连接、推送、拉取、冲突处理）

## 修改文件
- `AppViewModel.kt`     — 新增 `syncImport()` 方法（全量替换，正确处理删除操作）
- `ExportScreen.kt`     — 设置页顶部新增「GitHub 同步设置」入口卡片
- `Navigation.kt`       — 新增 `GitHubSync` 路由（不出现在侧边栏）
- `MainActivity.kt`     — 注册 `github_sync` NavHost 路由
- `app/build.gradle.kts`      — 新增 `androidx.security:security-crypto` 依赖
- `gradle/libs.versions.toml` — 新增 securityCrypto 版本定义

## 技术要点
- PAT 使用 EncryptedSharedPreferences（Android Keystore AES256）加密存储
- 冲突检测：基于 state.json 的 SHA-256 hash 比对，双端都有修改时弹出冲突对话框
- 推送时检测到冲突可选「覆盖远端」或「拉取远端」
- 拉取 = 全量替换（repo.importAll），保证删除操作正确同步
- 无新网络库依赖，复用项目已有的 Gson
