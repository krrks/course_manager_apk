#!build
# 知识点功能：课次关联知识点 + 知识点管理页

## 新增功能

### 知识点数据模型
- 新增 `KnowledgePoint` 域模型：grade / chapter / section / code / content / isCustom
- Room 新增 `knowledge_points` 表（数据库版本 2→3，含迁移脚本）
- `Lesson` 新增 `knowledgePointIds` 字段，记录本节课涉及的知识点 ID 列表

### 种子数据
- `assets/knowledge_points.json`：内置第1章·机械运动 19 个知识点（初中物理）
- 首次启动自动写入 Room，后续仅使用数据库，不再读取 asset

### 课次编辑（LessonFormDialog）
- 新增「涉及知识点」区域，支持多选
- 底部弹出选择器（KnowledgePointPickerSheet）：按章分组、学段过滤、关键词搜索
- 支持在选择器内直接添加新知识点（inline 表单）

### 课次详情（LessonDetailDialog）
- 新增「涉及知识点」展示区，以卡片形式显示已选知识点内容

### 知识点管理页（KnowledgePointsScreen）
- 导航菜单新增「知识点」入口（图标：Lightbulb）
- 支持按学段过滤、关键词搜索、按章分组展示
- 支持新增、编辑、删除知识点（自定义知识点 isCustom=true）

### 文件变动
- 新增：`assets/knowledge_points.json`
- 新增：`LessonKnowledgePointPicker.kt`
- 新增：`KnowledgePointsScreen.kt`
- 新增：`docs/knowledge_points_schema.md`
- 修改：`Models.kt` / `Entities.kt` / `Daos.kt` / `Mappers.kt`
- 修改：`AppDatabase.kt`（版本3 + migration）
- 修改：`AppRepository.kt` / `AppViewModel.kt` / `GsonModels.kt`
- 修改：`LessonDialogs.kt` / `Navigation.kt` / `MainActivity.kt`
