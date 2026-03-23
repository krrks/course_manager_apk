#!build
# StudentsScreen：编辑学生表单紧凑化 + 班级选择 Sheet

## 表单布局紧凑化
- 「姓名」+「学号」并排一行（各 ½）
- 「性别」+「年级」并排一行（各 ½）
- 头像行保持居中全宽

## 班级选择器重构
- 表单内只显示摘要行：「所在班级  已选 X 个  [选择]」
- 摘要行下方显示已选班级名称 chips（最多 6 个，超出显示 +N）
- 点击「选择」弹出 ModalBottomSheet，内含：
  - 年级筛选 chips（全部 / 高一…初三），横向滚动，单选
  - 搜索框（实时过滤班级名称，与年级叠加）
  - 全选 / 取消全选 / 清空已选快捷按钮
  - LazyColumn 班级列表（Checkbox + 班级名 + 年级 chip + 科目 chip）
  - 「确定」按钮关闭 Sheet 并回传结果

## 文件变更
- `app/src/main/java/com/school/manager/ui/screens/StudentsScreen.kt`
