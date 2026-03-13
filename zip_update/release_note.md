# Patch Notes

## 修复 1：科目显示错误（英语课显示为语文）

- **根因**：`ScheduleScreen` 点击课表格子创建记录时，代码先从 `cls.subject`（班级的默认科目字段）
  查找 `subjectId`，该字段可能与课程槽 `slot.subjectId` 不符，导致科目被替换为错误科目。
- **修复**：删除多余的 `subId` 推导，直接将 `slot` 原样传入 `AddAttendanceFromScheduleDialog`，
  使用 `slot.subjectId` 存入记录，保证科目始终与课表槽一致。
- **影响文件**：`ScheduleScreen.kt`

## 修复 2：时间输入改为「开始时间 + 时长」

- **改动**：将原来的"开始时间 / 结束时间"双字段改为"开始时间 + 时长"：
  - **开始时间**：保留时钟圆盘选择器
  - **时长预设**：1 小时 / 1.5 小时 / 2 小时（FilterChip，支持高亮当前选中值）
  - **手动输入**：小时 + 分钟两个数字输入框
  - **自动推算**：结束时间 = 开始时间 + 时长，在卡片底部实时显示
  - 默认时长 **1 小时**（60 分钟）
- **同时修复**：`AttendanceScreen` 的添加/编辑记录对话框复用同一个 `TimeRangeRow` 函数，无需额外改动。
- **影响文件**：`ScheduleScreen.kt`（`TimeRangeRow` 函数）
