#!build
# 修复编译错误：更新剩余屏幕对 state.lessons 的引用

## 问题
TeachersScreen、StudentsScreen、SubjectsScreen 仍引用已删除的
state.attendance 和 state.schedule 字段，导致编译失败。
AppViewModel 中 importFullBackupZip 和 parseGsonState 使用表达式体
函数但包含显式 return，Kotlin 不允许此写法。

## 修复内容

### TeachersScreen.kt
- lessonCount: state.attendance → state.lessons，通过 classes.headTeacherId 解析教师
- teacherClasses: state.schedule.any → state.classes.filter { c.headTeacherId == t.id }

### StudentsScreen.kt
- attended: state.attendance → state.lessons（保留 attendees + status 过滤）

### SubjectsScreen.kt
- schedCount / attCount: state.schedule / state.attendance → state.lessons
- 语义调整为"课次总数"和"已完成"（原"排课数"和"上课次数"在新模型中等价）

### AppViewModel.kt
- importFullBackupZip 和 parseGsonState 从表达式体改为块体，消除
  "Returns are prohibited for functions with an expression body" 错误
- mergeAll 中删除无效的 upsert 内部函数残留代码
