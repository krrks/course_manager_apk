#!build
# MonthView：月视图撑满全屏

将月视图的日期行由固定 68dp 高度改为 weight(1f) 均分，
LazyColumn 替换为普通 Column，格子改用 fillMaxHeight()，
月份网格现在自动占满整个屏幕高度。

## 文件变更
- `app/src/main/java/com/school/manager/ui/screens/LessonViews.kt`
