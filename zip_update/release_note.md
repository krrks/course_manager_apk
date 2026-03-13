# 修改说明

## 1. 设置页面 - 修复版本号显示

**文件**: `app/src/main/java/com/school/manager/ui/screens/ExportScreen.kt`

- 移除硬编码的 `APP_VERSION = "1.5.0"` 常量
- 改为使用 `BuildConfig.VERSION_NAME`（动态读取，与 `app/build.gradle.kts` 中的 `versionName` 保持同步）
- 版本号显示将始终与编译时的实际版本一致，不再出现版本号停留在旧值的问题

## 2. 添加上课记录页面 - 布局优化：日期与开始时间同行

**文件**: `app/src/main/java/com/school/manager/ui/screens/AttendanceScreen.kt`

- 在 `AttendanceFormDialog` 中，将 `DatePickerField`（日期）和 `TimeRangeRow`（开始时间+时长）用 `Row` 包裹，各占 50% 宽度
- 两个字段并排显示，节省垂直空间
- 保留所有现有逻辑和字段，仅调整布局结构
