# Patch Notes

## 修复 1：时间选择改为时钟圆盘

- **问题**：上课记录（及课程）添加时的时间滚轮无法正常工作
- **修复**：删除 `TimeWheelPicker`，改用 Material 3 内置 `TimePicker` 组件
  - 先点击小时圆盘选小时，自动跳转分钟圆盘选分钟，符合 Android 标准交互
  - `is24Hour = true`，支持 0-23 全天时间
  - 影响文件：`ScheduleScreen.kt`（`TimeRangeRow` / `TimePickerDialog`，AttendanceScreen 复用相同函数）

## 修复 2：其他页面 FAB 菜单跑到屏幕顶部

- **问题**：`ScreenSpeedDialFab` 展开时，速拨菜单出现在屏幕顶部而非右下角
- **根本原因**：全屏透明遮罩 `Box(fillMaxSize)` 与速拨 `Column` 是 Scaffold FAB 槽内的兄弟节点；
  遮罩撑满后 Column 被挤到屏幕顶端
- **修复**：用一个外层 `Box` 将遮罩和 Column 包裹在一起，
  展开时外层 Box `fillMaxSize`，Column 通过 `contentAlignment = Alignment.BottomEnd` 固定在右下角
- **影响文件**：`CommonComponents.kt`（`ScreenSpeedDialFab`）
