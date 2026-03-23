#!build
# Form Dialog UI：短字段合并为双列，布局更紧凑

## 改动内容

### LessonDialogs.kt — LessonFormDialog
- 科目只读展示 + 教师 dropdown 合并为一行（各 1/2）
- 无科目时教师仍独占全宽（fallback 不变）

### TeachersScreen.kt — TeacherFormDialog
- 性别 dropdown (1/3) + 手机输入框 (2/3) 合并为一行

### SubjectsScreen.kt — SubjectFormDialog
- 科目名称 (3/5) + 科目编号 (2/5) 合并为一行

### ClassesScreen.kt — ClassFormDialog
- 教师 dropdown (1/2) + 科目 dropdown (1/2) 合并为一行
- 科目提示文字保持全宽显示在行下方
