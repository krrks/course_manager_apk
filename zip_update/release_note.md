# 🛠 Bug Fix: 修复编译错误

## 修复内容

### 1. ExportScreen.kt — `Unresolved reference 'Box'`
- **问题**：`DropdownFilterChipLong` 函数中使用了不存在的 `androidx.compose.material3.Box`，导致编译失败，并引发后续多个 `@Composable invocations can only happen from the context of a @Composable function` 错误。
- **修复**：将 `androidx.compose.material3.Box { ... }` 改为 `Box { ... }`（正确来源为 `androidx.compose.foundation.layout.*`，已通过通配符导入）。

### 2. ScheduleScreen.kt — `No parameter with name 'confirmLabel' found`
- **问题**：`ScheduleDetailDialog` 调用 `FluentDialog` 时使用了 `confirmLabel = "编辑"`，但 `FluentDialog` 的实际参数名为 `confirmText`，导致编译失败，并引发后续多个 `@Composable invocations can only happen from the context of a @Composable function` 错误。
- **修复**：将 `confirmLabel = "编辑"` 改为 `confirmText = "编辑"`。

## 受影响文件
- `app/src/main/java/com/school/manager/ui/screens/ExportScreen.kt`
- `app/src/main/java/com/school/manager/ui/screens/ScheduleScreen.kt`
