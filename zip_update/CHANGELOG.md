#!build
# 详情对话框：短字段两列并排显示

新增 `DetailRowPair` 组件，将两个短字段并排放在同一行，
长字段（姓名、备注、课题、手机等）保持独占整行。

## 并排配对

| 对话框 | 并排字段 |
|--------|---------|
| 课次详情 | 科目 + 教师 / 日期 + 时间 |
| 班级详情 | 年级 + 科目（有科目时）/ 编制人数 + 在籍学生 |
| 教师详情 | 性别 + 完成课次 |
| 学生详情 | 性别 + 年级 |
| 科目详情 | 课次总数 + 已完成 |

## 文件变更
- `app/src/main/java/com/school/manager/ui/components/CommonComponents.kt`：新增 `DetailRowPair`
- `app/src/main/java/com/school/manager/ui/screens/LessonDialogs.kt`
- `app/src/main/java/com/school/manager/ui/screens/ClassesScreen.kt`
- `app/src/main/java/com/school/manager/ui/screens/TeachersScreen.kt`
- `app/src/main/java/com/school/manager/ui/screens/StudentsScreen.kt`
- `app/src/main/java/com/school/manager/ui/screens/SubjectsScreen.kt`
