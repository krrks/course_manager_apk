#!build
# 课次列表：新增筛选维度 + 多选批量操作

## 变更内容

### 新增文件
- `LessonListView.kt` — ListView 和 LessonCard 从 LessonViews.kt 拆出，新增多选逻辑
- `LessonBatchActionDialog.kt` — 针对多选课次的统一批量操作弹窗（修改时间/修改状态/删除）

### 修改文件
- `LessonFilterSheet.kt` — 新增三个筛选维度：日期范围、星期（周一~周日）、科目
- `LessonViews.kt` — 移除 ListView 和 LessonCard（已迁移至 LessonListView.kt）
- `LessonScreen.kt` — 接入新筛选参数、多选状态管理、BatchActionDialog

### 功能说明
筛选维度（全部视图生效）：
  - 原有：班级、状态、教师、学生
  - 新增：科目（多选）、星期（多选）、日期范围（开始/结束）

多选批量操作（仅列表视图）：
  - 点击列表头部 ☑ 按钮进入选择模式
  - 班级分组头部 Checkbox 可全选/取消该班级所有课次
  - 底部操作栏显示已选数量，点击「批量操作」弹出 BatchActionDialog
  - 三种操作：修改时间、修改状态、删除（删除需二次确认）
  - 切换视图或退出选择模式时自动清空选中

### 不变内容
- 周视图、月视图、日视图逻辑完全不变
- 原有 BatchModifyDialog / BatchDeleteDialog（按班级批量操作）保持不变
