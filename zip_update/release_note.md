# 修改说明 v2 — 编译错误修复

## 上版问题
1. `AttendanceFormDialog_patch.kt` 作为独立 .kt 文件被编译，没有 package/imports，产生大量 unresolved reference
2. `ScheduleScreen.kt` 中 `vm.addAttendance(att)` 传入了 Attendance 对象，但 ViewModel 签名要求展开的具名参数
3. `EditScheduleDialog(...)` 调用缺少 `onDismiss` 参数

---

## 本次修改文件

### 文件1：ScheduleScreen.kt（完整替换）
- **修复** `vm.addAttendance(...)` 调用：展开为 `a.classId, a.subjectId, a.teacherId, a.date, a.startTime, a.endTime, a.topic, a.status, a.notes, a.attendees, a.code`
- **修复** `EditScheduleDialog(...)` 调用：补全 `onDismiss = { editing = null }`
- **新增** `StartTimeField` 组件（internal，可跨同模块文件使用）
- **新增** `InlineDurationPicker` 组件（internal）
- **重构** `AddScheduleDialog`、`EditScheduleDialog`、`AddAttendanceFromScheduleDialog` 布局（详见上版说明）
- **保留** `TimeRangeRow`（供 AttendanceScreen 继续使用）

### 文件2：AttendanceScreen_FormDialog_PATCH.kt（说明文件，非独立编译）
- **用途**：说明将 `AttendanceScreen.kt` 中 `AttendanceFormDialog` 函数整体替换为新版本
- **操作**：在 `AttendanceScreen.kt` 中找到 `@Composable internal fun AttendanceFormDialog`，
  将整个函数（到其最后一个 `}`）替换为 patch 文件中的代码
- **不要**把此文件直接放入 src 目录（会导致重复符号编译错误）

## 删除文件
- 删除上版误放入 src 的 `AttendanceFormDialog_patch.kt`

---

## 布局优化效果（同上版）
- 添加课程：原 6行 → 4行（-35%）
- 添加上课记录（两处）：原 7-8行 → 5-6行（-30%）
