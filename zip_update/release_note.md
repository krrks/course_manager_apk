# UI 修复：时长 chips 换行问题

## 问题
「添加上课记录」Dialog 中，「课题⅔ + 时长⅓」并排一行时，
时长列宽度不足，导致「1.5h」等 chip 文字纵向换行，显示异常。

## 修改文件
- `app/src/main/java/com/school/manager/ui/screens/ScheduleScreen.kt`
- `app/src/main/java/com/school/manager/ui/screens/AttendanceScreen.kt`

## 修改内容
将原来的「行4：课题⅔ + 时长⅓」Row 拆为两行独立排列：
1. 时长（DurationChipsCompact，全宽）
2. 课题（FormTextField，全宽）

其余所有布局（行1~3、科目 badge、出勤学生、备注）保持不变。

两个 Dialog 同步修改：
- `AddAttendanceFromScheduleDialog`（ScheduleScreen.kt）
- `AttendanceFormDialog`（AttendanceScreen.kt）
