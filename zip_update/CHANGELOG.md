#!build
# ClassesScreen：班级列表新增年级/教师/科目筛选

参考学生页面，在班级列表顶部新增横向可滚动筛选行：
- 年级筛选（全部年级 / 高一…初三）
- 教师筛选（全部教师 / 各教师）
- 科目筛选（全部科目 / 各科目）

三个筛选条件叠加生效，筛选行下方显示「共 X 个班级」计数。

## 文件变更
- `app/src/main/java/com/school/manager/ui/screens/ClassesScreen.kt`
