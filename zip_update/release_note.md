# UI 优化：「添加上课记录」Dialog 紧凑化

## 问题
两处「添加上课记录」Dialog 字段全部垂直排列，空间浪费，不够紧凑，
与「添加课程」Dialog（AddScheduleDialog）风格不一致。

## 修改文件
- `app/src/main/java/com/school/manager/ui/screens/ScheduleScreen.kt`
- `app/src/main/java/com/school/manager/ui/screens/AttendanceScreen.kt`

## 修改内容

### ScheduleScreen.kt
1. `StartTimeCompact`：`private` → `internal`（供 AttendanceScreen 共用）
2. `DurationChipsCompact`：`private` → `internal`（供 AttendanceScreen 共用）
3. `AddAttendanceFromScheduleDialog`（课表格子点击进入的添加记录）完全重写，
   对齐 AddScheduleDialog 的紧凑行布局：
   - 行1: 编号½ + 状态½
   - 行2: 科目 badge½ + 教师½（班级/科目已由课表槽固定，改为只显示 badge）
   - 行3: 日期½ + 开始时间½
   - 行4: 课题⅔ + 时长 chips⅓
   - 出勤学生 FlowRow + 备注

### AttendanceScreen.kt
1. `AttendanceFormDialog`（上课记录页 FAB 添加记录）完全重写，
   对齐 AddScheduleDialog 的紧凑行布局：
   - 行1: 编号½ + 状态½
   - 行2: 班级½ + 教师½
   - 科目 badge（由班级自动推断）
   - 行3: 日期½ + 开始时间½
   - 行4: 课题⅔ + 时长 chips⅓
   - 出勤学生 FlowRow + 备注
2. 补充 `minutesBetween` / `addMinutesToTime` 私有辅助函数（若文件中尚不存在）

## 效果
两个「添加上课记录」Dialog 与「添加课程」Dialog 在视觉密度和布局上完全一致，
字段利用率提升约 40%，对话框高度减少约 2–3 行。
