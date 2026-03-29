#!build
# 设置页重构：知识点导入导出支持 + 旧版兼容代码清理

## 主要改动

### 1. 知识点完整纳入备份体系（BackupModels / BackupManager）
- BackupCounts 新增 kpChapters / kpSections / knowledgePoints 三个字段
- BackupMeta schemaVersion 升至 3（标识此格式含 KP 数据）
- BackupManager.buildFullZip：完整备份现在包含全部知识点数据（包括自定义）
- 筛选备份依然不含知识点（知识点是课程级数据，不属于某次课次筛选）

### 2. 导入预览对话框（ExportImportDialog）
- 若备份中含知识点，在预览中额外显示「章 / 节 / 知识点」三行数据量
- 不含知识点的备份（筛选导出）不显示该区块

### 3. 设置页面 UI 重构（ExportScreen）
- 顶部新增三个统计 badge：课次数 / 知识点数 / 自定义知识点数
- 导出/导入卡片样式统一，移除内联 IoCard 私有组件，改用通用 SectionCard
- 筛选备份从独立卡片改为折叠面板（条件选择 + 导出按钮在同一 Surface 内）
- 危险操作「重置」改为 AlertDialog 二次确认，取代原来的两步按钮方案
- 移除筛选导出中多余的日期筛选提示文字
- 移除 IoCard 私有组件（逻辑合并到 SectionCard）

### 4. 数据库版本 2 → 3（AppDatabase）
- 配合此次 KP 数据完整化，版本号升至 3
- 继续使用 fallbackToDestructiveMigration（第一版，无需保留旧数据）

### 5. GsonModels 清理
- 移除 parseGsonState 中针对旧版备份的向后兼容处理（title 字段缺失时的降级逻辑）
- 使用 runCatching 统一包裹整个解析过程，更简洁

### 6. AppRepository 清理
- clearAll 重命名为 resetAll，并同时清除 KP 表
- importAll：reset 后若无 KP 数据则自动重新 seed 内置知识点
- 移除旧版 SharedPreferences 迁移相关注释和空函数

### 7. AppViewModel init 逻辑改进
- 首次启动（isEmpty）：importAll(sampleData) → 内部自动 seed KP
- 升级启动（非空）：单独调用 seedKnowledgePoints()（幂等，已有数据则跳过）
