#!build
# 对话框布局精简：减少空间占用

## 全局
- `FluentDialog` 列间距 10→6 dp
- `DetailRow` 上下内边距 8→5 dp
- `SectionHeader` 上下内边距 6→3 dp

## LessonDetailDialog
- 「快速更改状态」FlowRow（5 个 FilterChip，~80-100 dp）移除
- 状态行改为内联下拉菜单（点击 StatusChip 旁的箭头即可更改）
- 上课进度块合并为紧凑 Column（原来的独立进度条行 + 分隔线折叠进同一容器）

## LessonFormDialog
- 科目 Surface badge（独占一行）改为紧凑的 `Text + ColorChip` 行
- 出勤学生 FlowRow 水平间距 6→4 dp

## BatchGenerateDialog
- 移除「重复类型」SectionHeader，改为行内「重复」小标签
- 移除「星期」SectionHeader，改为行内标签
- 移除「跳过日期」SectionHeader（按钮文字已自解释）

## BatchModifyDialog
- 移除「修改时间（留空则不修改）」SectionHeader
- 合并提示文字为单行

## BatchDeleteDialog
- 警告 Surface 内边距 12/8→10/6 dp

## ClassFormDialog
- 「暂无科目」全宽 Surface 改为单行 labelSmall 提示文字

## 文件变更
- `app/src/main/java/com/school/manager/ui/components/CommonComponents.kt`
- `app/src/main/java/com/school/manager/ui/screens/LessonDialogs.kt`
- `app/src/main/java/com/school/manager/ui/screens/LessonBatchDialogs.kt`
- `app/src/main/java/com/school/manager/ui/screens/ClassesScreen.kt`
