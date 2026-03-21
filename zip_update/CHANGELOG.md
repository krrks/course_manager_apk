#!build
# 课程日历：缩放、更多信息、列表星期字段

## 1. 周视图 / 日视图 — FAB 缩放按钮
- 新增 4 档缩放：60 / 80（默认）/ 100 / 120 dp/小时
- 仅在 week / day 视图时在 FAB 菜单中显示「放大」「缩小」按钮

## 2. 周视图格子 — 增加班级名称
- 在科目 / 时间 / 课次三行之后追加班级名称（小字，截断）

## 3. 日视图格子 — 增加出勤人数 + 备注
- 出勤人数：`👥 N 人出勤`（有出勤记录时显示）
- 备注预览：`💬 ...`（备注非空时显示，单行截断）

## 4. 列表视图 — 日期列增加星期字段
- 日期列（日 / 月）下方新增第三行：`周X`（蓝色加粗小字）

## 文件变更
- `LessonTimeHelpers.kt`：DP_PER_HOUR 改为 ZOOM_LEVELS 档位常量；
  minuteOffsetDp / durationDp / calTotalHeight 接受 dpPerHour 参数
- `LessonViews.kt`：WeekView 接受 dpPerHour；DayView 迁出至新文件
- `LessonDayView.kt`：新文件，承接 DayView，增加出勤 + 备注行
- `LessonScreen.kt`：zoomIdx 状态；FAB 缩放按钮；传 dpPerHour
- `LessonListView.kt`：LessonCard 日期列加星期行
