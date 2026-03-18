#!build
# 修复编译错误（patch v2）

## 修复的编译错误

**ClassesScreen.kt**
- `GradeBadge` 不存在 → 改回 `ColorChip(cls.grade, gradeCol)`

**AppViewModel.kt**
- `importFullBackupZip` 函数名恢复（ExportScreen 调用此名称）
- `importFromZip` 改为块体函数，修复 "Returns are prohibited for functions with an expression body" 错误

**Models.kt**
- 新增 `Attendance.resolvedSubjectName(subjects, classes)` 扩展函数
  （原来只有 `Schedule` 有此扩展，`AttendanceScreen` 调用时 receiver 类型不匹配）

**ScheduleScreen.kt（via apply.sh）**
- Bug-4: 列表视图 `subjectName` 和日历视图 label 改用 `slot.resolvedSubjectName()`
  修复 `resolvedSubName` / `resolvedSubName2` 未定义的编译错误

**AttendanceScreen.kt（via apply.sh）**
- Bug-5: AttendanceCard 标题及日历视图 label 改用 `rec.resolvedSubjectName()`

## 完整 Bug 修复清单（同上一个 patch）

- Bug 1 & 2: 编辑班级科目后课表/上课记录级联更新
- Bug 3: 班级科目改为纯下拉选择
- Bug 4: 课表列表/日历统一用 resolvedSubjectName
- Bug 5: 上课记录列表统一用 resolvedSubjectName
- Bug 6: 删除科目后清空 legacy subject 字符串
