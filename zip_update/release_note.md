# Release Notes — v1.5.0 / 更新说明 — v1.5.0

## Bug Fixes / 修复内容

### 1. 退出后数据丢失 / Data loss on exit
修复退出 App 后数据消失的问题。根本原因：`AppViewModel.save()` 被 `viewModelScope.launch` 包裹，进程退出前协程来不及执行。现改为直接在主线程调用 `prefs.edit().apply()`，每次数据变更后立即写入。

Fixed: `save()` was wrapped in a coroutine and lost on process kill. Now calls `apply()` directly on the main thread.

**Files:** `AppViewModel.kt`

---

### 2. 设置页底部显示版本号 / Version number in Settings
在设置页（ExportScreen）底部新增版本号标注：`智慧课务管理 v1.5.0`。

Added version label at the bottom of the Settings screen.

**Files:** `ExportScreen.kt`

---

### 3. 版本号更新 / Version bump
`versionCode` 1 → 5，`versionName` 1.0.0 → 1.5.0

**Files:** `app/build.gradle.kts`
