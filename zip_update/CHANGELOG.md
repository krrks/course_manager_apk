#!build
# 知识点拆分：简短标题 + 完整内容

## 变更说明

### 数据模型
- `KnowledgePoint` 新增 `title` 字段（简短标题，如"摄氏温度两个标准"）
- 原 `content` 字段保留为完整解释文字
- `displayTitle` 计算属性：有 title 则用 title，否则截取 content 前20字
- Room 数据库版本从 1 升至 2，新增 `MIGRATION_1_2`（ALTER TABLE 添加 title 列，默认空字符串，旧数据不受影响）
- 旧版备份导入时 title 字段缺失会自动回退为空字符串，displayTitle 自动降级

### 知识点页面（KnowledgePointsScreen / KnowledgePointsDialogs）
- 知识点卡片：标题粗体显示 + 完整内容灰色小字在下方
- 添加/编辑表单：新增"简短标题"输入框，"完整内容"输入框独立保留
- 详情弹窗：分行显示标题和完整内容

### 上课记录（LessonDialogs / LessonKnowledgePointPicker）
- 课次详情和编辑表单的知识点区域：只显示 `[code] displayTitle`，不显示完整内容
- 选择器 PointRow：粗体显示 displayTitle，灰色小字显示 content（有 title 时才显示）
- 内联添加表单：新增"简短标题"字段

### JSON 资产（knowledge_points.json）
- 所有内置知识点补充了 `title` 字段
- 格式保持兼容，title 字段可选
