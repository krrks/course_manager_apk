# Release Notes — v1.5.1 / 更新说明 — v1.5.1

## 根本原因分析 / Root Cause Analysis

数据丢失的真正根源有两个，之前的修复只解决了表面问题：

**原因 1：`AppState` 默认值是示例数据（最关键）**
```kotlin
// 旧代码（Models.kt）—— 致命问题
data class AppState(
    val subjects: List<Subject> = sampleSubjects,   // ← Gson 反序列化时
    val teachers: List<Teacher> = sampleTeachers,   //    会用 sampleData 填充缺失字段！
    ...
)
```
Gson 在反序列化 `AppState` 时，若某个字段解析为 null（例如类型不匹配、版本升级导致结构变化），
会调用 `AppState` 的默认构造函数值，即 `sampleSubjects` 等示例数据，**覆盖掉已保存的数据**。

**原因 2：`save()` 使用 `apply()`（异步）而非 `commit()`（同步）**
`apply()` 会将写操作放到后台线程，强制关闭 App 时后台线程可能尚未完成，导致本次修改丢失。

## 修复内容 / Fixes

### 1. `Models.kt` — `AppState` 默认值改为空列表
```kotlin
// 新代码 —— 安全
data class AppState(
    val subjects: List<Subject> = emptyList(),
    val teachers: List<Teacher> = emptyList(),
    ...
)
fun sampleAppState(): AppState = AppState(sampleSubjects, sampleTeachers, ...)
```

### 2. `AppViewModel.kt` — `load()` 重写，`save()` 改用 `commit()`
- `load()` 改用 `GsonState`（全字段可空）解析 JSON，再手工构建 `AppState`，彻底隔离默认值
- 首次安装（prefs 无数据）时填入示例数据并保存
- `save()` 改为 `commit()`（同步写入），强制退出前确保数据已落盘

**Files changed:**
- `app/src/main/java/com/school/manager/data/Models.kt`
- `app/src/main/java/com/school/manager/viewmodel/AppViewModel.kt`
