#!build
# ClassesScreen：编辑班级表单紧凑化 + 学生选择 Sheet

## 表单布局紧凑化
- 「班级名称」+「班级编号」并排一行（⅔ + ⅓）
- 「年级」+「编制人数」并排一行（各 ½）
- 「教师」「科目」保持全宽

## 学生选择器重构
- 表单内只显示摘要行：「班级学生  已选 X 人  [选择]」
- 摘要行下方显示已选学生姓名 chips（最多 8 个，超出显示 +N）
- 点击「选择」弹出 ModalBottomSheet，内含：
  - 年级筛选 chips（全部 / 高一…初三），横向滚动，单选
  - 搜索框（实时过滤姓名，与年级叠加）
  - 全选 / 取消全选 / 清空已选快捷按钮
  - LazyColumn 学生列表（Checkbox + 姓名 + 年级/性别 chip）
  - 「确定」按钮关闭 Sheet 并回传选中结果

## 文件变更
- `app/src/main/java/com/school/manager/ui/screens/ClassesScreen.kt`
