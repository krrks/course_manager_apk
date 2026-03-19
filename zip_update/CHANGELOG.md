#!build
# 代码重构：大文件拆分，提升可维护性

## 变更内容

### LessonScreen.kt 拆分为 5 个文件
- `LessonScreen.kt` — 仅保留主入口 `LessonScreen()` 和过滤逻辑
- `LessonViews.kt` — `WeekView` / `MonthView` / `DayView` / `ListView` / `LessonCard`
- `LessonDialogs.kt` — `LessonDetailDialog` / `LessonFormDialog`
- `LessonBatchDialogs.kt` — `BatchGenerateDialog` / `BatchModifyDialog` / `BatchDeleteDialog`
- `LessonFilterSheet.kt` — `FilterBottomSheet`
- `LessonTimeHelpers.kt` — 日历常量、时间计算函数、`StatusChip` / `ViewSwitchIcons` / `StartTimeCompact` / `DurationChipsCompact` / `DatePickerField`

### AppViewModel.kt 拆分为 2 个文件
- `GsonModels.kt` — `GsonTeacher` / `GsonClass` / `GsonLesson` / `GsonState` 及 `parseGsonState()` 函数
- `AppViewModel.kt` — 去除 Gson 私有数据类，调用 `parseGsonState()` 完成导入解析

### CommonComponents.kt 拆分为 3 个文件
- `CommonComponents.kt` — 基础 UI 组件：`FluentCard` / `SectionHeader` / `ColorChip` / `DetailRow` / `EmptyState` / `FluentProgressBar` / `FluentDialog` / `FormTextField` / `FormDropdown` / `DropdownFilterChip`
- `AvatarComponents.kt` — `AvatarCircle` / `AvatarWithImage` / `StatusBadge`
- `SpeedDialFab.kt` — `SpeedDialItem` / `ScreenSpeedDialFab`

### 无功能变更
纯结构重构，所有逻辑、UI 行为与原版本完全一致。
