#!build
# 知识点三表重构：Chapter → Section → Point，编号自动生成

## ⚠ 迁移说明
- 数据库从 v3 升级到 v4
- 旧的扁平 knowledge_points 表被替换为三张关联表（kp_chapters / kp_sections / knowledge_points）
- 已有自定义知识点在此次迁移中丢失（一次性影响）；内置种子数据自动重新写入

## 数据层变更
- Models.kt：新增 KpChapter、KpSection、KpFull；KnowledgePoint 改为 sectionId + no；AppState 新增 kpChapters / kpSections
- Entities.kt：新增 KpChapterEntity、KpSectionEntity；KnowledgePointEntity 重构
- Daos.kt：新增 KpChapterDao、KpSectionDao
- Mappers.kt：新增对应 mapper
- AppDatabase.kt：v3→v4，MIGRATION_3_4 重建三张 KP 表
- AppRepository.kt：8 路 Flow combine；新增章/节 CRUD；种子改读新 JSON 格式
- AppViewModel.kt：新增 addKpChapter / addKpSection；knowledgePointFull() 联合查询；addKnowledgePoint 新签名
- GsonModels.kt：新增 GsonKpChapter / GsonKpSection；GsonKnowledgePoint 重构
- knowledge_points.json：改为 {chapters, sections, points} 三段式格式

## UI 变更
- KnowledgePointsScreen.kt：三级可折叠卡片（章 → 节 → 知识点）；SpeedDial 支持添加章/节/知识点；编号由 chapter.no.section.no.point.no 自动生成
- KnowledgePointsDialogs.kt（新文件）：ChapterFormDialog / SectionFormDialog / PointFormDialog 及对应详情 dialog
- LessonKnowledgePointPicker.kt：新增 allChapters / allSections 参数；章节筛选 chips 用真实 ID；行内添加表单改为选章→选节→填号+内容
- LessonDialogs.kt（apply.sh 补丁）：vm.knowledgePointFull、KpFull 字段引用、onAddNew 签名更新
