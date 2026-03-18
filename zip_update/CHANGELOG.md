#!build
# 修复6个数据同步Bug + 班级科目改为纯下拉选择

## 问题修复

**Bug 1 & 2 — 编辑班级改科目后，课表和上课记录不更新**
- `AppViewModel.updateSchoolClass()` 现在在更新班级 subjectId 时，
  同步级联更新该班级下所有 Schedule 和 Attendance 行的 subjectId，
  确保课表页和上课记录页立即显示新科目。

**Bug 3 — 班级编辑"科目"改为纯下拉选择（不允许自由输入）**
- `ClassesScreen` 中科目字段从 `AutocompleteTextField`（允许自由输入）
  改为 `DropdownField`（只能从现有科目列表中选择），彻底消除
  subjectId 因名称匹配失败而静默丢失的问题。
- 无科目时显示提示，引导用户前往"科目管理"页面添加。
- 移除 `FluentComponentAliases.kt` 中 `AutocompleteTextField` 的使用依赖
  （函数本身保留，不影响编译）。

**Bug 4 — 课表列表/日历视图科目名显示不走统一解析**
- `ScheduleScreen` 列表视图和日历视图的科目名展示，
  统一改用 `slot.resolvedSubjectName(subjects, classes)`
  （优先 subjectId FK 查 Subject 表，避免使用可能过时的 cls.subject 字符串）。

**Bug 5 — 上课记录列表科目名同类问题**
- `AttendanceScreen` 的 `AttendanceCard` 科目显示改用
  `rec.resolvedSubjectName(vm.state.value.subjects, vm.state.value.classes)`，
  保持与课表页一致的解析链路。

**Bug 6 — 删除科目后课表/记录页仍显示旧科目名**
- `AppViewModel.deleteSubject()` 在执行删除前，
  先清空所有关联班级的 legacy subject 字符串字段，
  避免 Room SET_NULL 触发后 fallback 显示旧名称。

## 修改文件

- `app/src/main/java/com/school/manager/viewmodel/AppViewModel.kt`
- `app/src/main/java/com/school/manager/ui/screens/ClassesScreen.kt`
- `app/src/main/java/com/school/manager/ui/screens/ScheduleScreen.kt` (via apply.sh)
- `app/src/main/java/com/school/manager/ui/screens/AttendanceScreen.kt` (via apply.sh)
