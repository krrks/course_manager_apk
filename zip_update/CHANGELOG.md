#!build
# UI调整：课次列表、时长默认值、班级教师字段、AI规则

## 变更内容

### 修改文件
- `LessonListView.kt` — 删除班级组头的「批量修改」「批量删除」按钮
- `LessonBatchActionDialog.kt` — 「修改时间」分支加入时长选择（DurationChipsCompact）
- `LessonTimeHelpers.kt` — 默认时长 45 分钟 → 2 小时
- `LessonDialogs.kt` — 默认结束时间 08:45 → 10:00
- `LessonBatchDialogs.kt` — 默认结束时间 08:45 → 10:00
- `ClassesScreen.kt` — 所有「班主任」显示文字改为「教师」
- `README.md` — 新增三步工作流 AI 规则及推理与代码分离规则
