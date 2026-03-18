#!build
# 修复 resolvedSubName / resolvedSubName2 编译错误（patch v3）

## 问题

前两次 patch 的 apply.sh 字符串替换均失败，导致 ScheduleScreen.kt 中
`resolvedSubName` / `resolvedSubName2` 变量引用残留（由第一次 patch 插入），
但对应变量声明从未成功插入。

## 本次修复

**apply.sh 改用 `grep -n` 精确定位行号 + `sed -i` 按行号替换**，
完全不依赖字符串 pattern 匹配，直接覆盖有问题的行：

- ScheduleScreen.kt 第 ~346 行（日历视图 label）→ `slot.resolvedSubjectName(...)`
- ScheduleScreen.kt 第 ~404 行（列表视图 subjectName）→ `slot.resolvedSubjectName(...)`
- AttendanceScreen.kt AttendanceCard title → `rec.resolvedSubjectName(...)`

其他文件（AppViewModel、ClassesScreen、Models.kt）已由上次 patch 正确更新，
本次不重复打包。
