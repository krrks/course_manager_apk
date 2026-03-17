#!build
# UI: 时长选择器改为 +/- 按钮，移除预设 1h/1.5h/2h chips

## 问题
添加上课记录 Dialog 第三行中，时长区域含「时长」标签 + 三个 chips (1h/1.5h/2h) +
时输入框 + 分输入框，空间不足导致「分」文本框显示不完整。

## 修改文件
- `app/src/main/java/com/school/manager/ui/screens/ScheduleScreen.kt`

## 修改内容
替换 `DurationChipsCompact` 函数（及其别名 `InlineDurationPicker`）：
- 移除预设时长按钮 1h / 1.5h / 2h
- 保留「时长」标签、时输入框、分输入框
- 在分输入框后添加「−」「+」按钮，步长 10 分钟
- 默认时长改为 2h（120 分钟）
- 两处 Dialog 同步修复：AddAttendanceFromScheduleDialog（课表页）和 AttendanceFormDialog（上课记录页）
