# 修改说明 — 编号/时间宽度改为 1:1

## 问题
AddScheduleDialog 和 EditScheduleDialog 中，「编号 + 时间」行的宽度比例为 2:1，
导致编号列过宽、时间字段过窄。

## 修改内容
**文件：** `app/src/main/java/com/school/manager/ui/screens/ScheduleScreen.kt`

两处修改（AddScheduleDialog、EditScheduleDialog 各一处）：
- 「编号」列 `weight(2f)` → `weight(1f)`
- 「时间」列保持 `weight(1f)` 不变
- 注释更新：「2/3 + 1/3」→「1/2 + 1/2」

效果：编号和开始时间各占对话框宽度的 50%，宽度均等。
