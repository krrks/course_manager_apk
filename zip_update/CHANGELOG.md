#!build
# GitHub sync: skip built-in KPs, only sync custom KPs when changed

## 变更概述
GitHub 同步不再传输 237 条内置知识点，大幅减少每次推送/拉取的数据量。
自定义知识点单独存放在 `userdata_sync/kp_custom.json`，
仅在内容发生变化时才推送或拉取。

## 同步策略
- `state.json` — 课次、班级、教师、学生、科目（不含任何知识点）
- `kp_custom.json` — 仅用户自定义知识点（isCustom=true）
- 推送时对 `kp_custom.json` 做 SHA-256 比对，未变化则跳过上传
- 拉取时读取 `meta.json` 中的 `kpCustomHash`，与本地缓存 hash 比对，
  仅在不同时才下载 `kp_custom.json`
- 内置知识点（`isCustom=false`）永远不参与同步，由本地 `KnowledgePointsData` 保证

## 拉取行为变化
- 拉取不再调用 `syncImport`（全量替换含KP），改为：
  1. `syncImportStateOnly` — 替换非KP数据，保留本地KP表
  2. `syncMergeCustomKps`  — 仅在 hash 不同时替换自定义KP

## 修改文件
- `GitHubSyncViewModel.kt` — 核心推送/拉取逻辑
- `GitHubSyncService.kt`   — GitHubMeta 新增 kpCustomHash、customKpCount 字段
- `GsonModels.kt`          — 新增 GsonCustomKps、parseCustomKps
- `Daos.kt`                — KnowledgePointDao 新增 deleteAllCustom 查询
- `AppRepository.kt`       — 新增 importStateOnly、mergeCustomKps
- `AppViewModel.kt`        — 新增 syncImportStateOnly、syncMergeCustomKps
- `GitHubSyncScreen.kt`    — pull 回调更新为双参数签名，更新说明文字
