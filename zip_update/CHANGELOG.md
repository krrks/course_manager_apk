#!build
# 课表筛选标题移至屏幕顶部

## 问题
筛选教师/学生后，"xx的课表"标题文字通过 `Box + Align.TopCenter` 叠加在日历视图内部顶部，
导致标题与日历网格重叠，视觉混乱。

## 修改文件
- `app/src/main/java/com/school/manager/ui/screens/ScheduleScreen.kt`

## 修改内容
将 Scaffold body 从 `Box` 改为 `Column` 布局：
1. 顶部：筛选标题栏（`FluentBlueLight` 背景，仅在有筛选时显示）
2. 下方：日历/列表视图（占剩余空间 `weight(1f)`）

原来的叠加式 `Box + align(Alignment.TopCenter)` 已移除。
其余所有代码（FAB、dialog、CalendarGrid、ScheduleListView 等）完全不变。
