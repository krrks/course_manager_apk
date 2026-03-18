#!build
# 修复 Plan A 编译错误：替换四个 Screen 文件 + 修复 AppViewModel

## 问题原因
上一次 patch 的 apply.sh 正则替换未能成功修改 Screen 文件，导致以下错误残留：
- AttendanceScreen: `state` 未定义、`a.subjectId` 引用已删字段、`cls.subject` 残留
- ScheduleScreen: AddScheduleDialog 有两个 `tId` 声明、`subjectId`/`cls.subject` 残留
- ClassesScreen/TeachersScreen: `cls.subject`/`c.subject` 残留
- AppViewModel: `repo.snapshot()` suspend 函数在非协程中调用、expression body 语法错误

## 本次修复
直接替换五个文件（无 apply.sh，文件即最终版本）：
- `AttendanceScreen.kt` — 所有视图函数补上 `state: AppState` 参数；科目通过 `resolvedSubject()` 推导
- `ScheduleScreen.kt` — AddScheduleDialog/EditScheduleDialog 重写；`subjectId` 全部移除
- `ClassesScreen.kt` — 去掉 `c.subject` 传参；`addSchoolClass` 只传 `subjectId`
- `TeachersScreen.kt` — `c.subject` 改为 `c.resolvedSubject(state.subjects)?.name`
- `AppViewModel.kt` — `exportFullBackupZip` 改为 block body；`repo.snapshot()` 改为 `state.value`
