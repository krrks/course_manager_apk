#!build
# UI: 添加课程/编辑课程 Dialog — 星期与时长拆为两行

## 问题
AddScheduleDialog 和 EditScheduleDialog 中，「星期(1/3) + 时长(2/3)」并排一行，
时长区域（时长标签 + 时输入框 + 分输入框 + − + + 按钮 + 结束时间提示）宽度不足，
导致最右侧「+」按钮被截断，无法完整显示。

## 修改文件
- `app/src/main/java/com/school/manager/ui/screens/ScheduleScreen.kt`

## 修改内容
将原来的「行3：星期(1/3) + 时长(2/3)」Row 拆为两行独立排列：
1. 星期（FormDropdown，全宽）
2. 时长（DurationChipsCompact，全宽）

其余所有布局（行1班级+教师、行2编号+时间、科目badge、确认/删除按钮）保持不变。
AddScheduleDialog 和 EditScheduleDialog 同步修改。
