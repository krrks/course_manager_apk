#!build
# 移除旧版兼容代码 + 修复课次编辑页知识点选择器为空的问题

## 变更说明

### 1. 移除旧版数据库兼容代码
- AppDatabase.kt: 删除 MIGRATION_2_3 和 MIGRATION_3_4，数据库版本重置为 1（当前三表结构作为唯一基准版本）
- 使用 fallbackToDestructiveMigration() 处理任何版本不匹配

### 2. 知识点 ID 稳定性说明
知识点已通过 PRIMARY KEY Long ID 与课次绑定（存储在 lesson.knowledgePointIds 中）。
只要知识点记录的 ID 不变，无论内容如何排序、重排，课次引用始终准确。
JSON 种子文件中的 ID 为固定数值（1001–3005），保证稳定。

### 3. 修复课次编辑页知识点选择器为空
- LessonDialogs.kt: LessonFormDialog 中调用 KnowledgePointPickerSheet 时，
  将原来错误的 emptyList() 替换为 state.kpChapters / state.kpSections / state.knowledgePoints，
  使选择器能正确显示所有已存在的知识点。

### 4. GsonModels.kt 清理
- 移除对旧版扁平 KnowledgePoint 格式（grade/chapter/section/code 字段）的引用
- 保持与当前三表格式（sectionId + no）完全一致
