#!build
# 修复数据结构：科目外键化，消除冗余关联

## 问题修复

**问题 1（根因）：SchoolClass.subject 改为外键**
- `SchoolClass` 新增 `subjectId: Long?` 字段，作为指向 `Subject` 的规范外键
- 保留 `subject: String` 作为 legacy 展示兜底和向后兼容
- `updateSubject()` 时自动级联更新所有关联班级的 `subject` 字符串
- 班级编辑表单中"科目"字段改为从 Subject 列表下拉选择，选中后同时写 `subjectId` 和 `subject`
- `AttendanceScreen`、`ScheduleScreen` 中的 `sId` 解析优先用 `cls.subjectId`，再 fallback 到字符串匹配

**问题 2：去除双向冗余 Teacher.subjectIds**
- `Teacher` 数据类删除 `subjectIds: List<Long>` 字段
- 教师所教科目改为从 `Subject.teacherId` 动态派生（单一来源）
- `TeachersScreen` 中任教科目 chips 改为 `state.subjects.filter { it.teacherId == t.id }`
- 旧备份中的 `subjectIds` 字段在 `GsonTeacher` 中保留 nullable，反序列化时忽略，向后兼容

**问题 3：teacherId 冗余存储（部分改善）**
- `Schedule` 和 `Attendance` 中的 `teacherId` 保留（排课/记录层面需要独立记录授课教师）
- `ScheduleScreen` / `AttendanceScreen` 中的科目 badge 改为通过 `subjectId` FK 查名称

**问题 4：向后兼容数据迁移**
- `load()` 和 `importMerge()` 在读取旧数据时，若 `subjectId` 为 null，自动通过 subject 字符串在 Subject 列表中匹配并填充 `subjectId`
- 所有现有数据无需手动迁移

## 修改文件
- `app/src/main/java/com/school/manager/data/Models.kt`
- `app/src/main/java/com/school/manager/viewmodel/AppViewModel.kt`
- `app/src/main/java/com/school/manager/ui/screens/ClassesScreen.kt`
- `app/src/main/java/com/school/manager/ui/screens/TeachersScreen.kt`
- `app/src/main/java/com/school/manager/ui/screens/AttendanceScreen.kt` (via apply.sh)
- `app/src/main/java/com/school/manager/ui/screens/ScheduleScreen.kt` (via apply.sh)
