#!build
# 移除内置数据，按学段分拆 KP 同步文件，支持自定义学段

1. 移除 APK 内置示例数据（班级/教师/学生/课次）；首次启动为空白状态
2. 移除内置知识点（237 条）；KnowledgePointsData.kt 及 knowledge_points.json 一并删除
3. 「重置为示例数据」→「清空所有数据」，清空 DB 中全部表
4. GitHub 同步：自定义知识点按学段拆分为独立文件 kp_custom_{学段}.json
5. meta.json 新增 gradeKpHashes 字段（grade→SHA-256 map），兼容旧 kpCustomHash 字段回退
6. 学段支持自由输入（章节表单改为自动补全），不再限定「初中/高中」
7. 知识点屏/选择器中的学段筛选标签从现有章节表动态派生
