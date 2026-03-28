#!build
# 彻底修复知识点不显示问题：移除 asset 文件依赖，改为代码内硬编码

根本原因分析：
asset 文件读取在 Room 初始化完成前可能失败，且 JSON 解析链路涉及多个
可能出错的环节（文件读取、字符集、JSON 解析），任意一处失败都导致整个
seed 静默跳过。

修改方案：
1. 新增 KnowledgePointsData.kt — 将全部 12 章 47 节 200+ 个知识点
   直接硬编码为 Kotlin 数据，完全消除 asset 文件读取和 JSON 解析环节。

2. AppRepository.kt — seedKnowledgePoints() 不再接受 Context 参数，
   不再读取 assets/knowledge_points.json，直接遍历 KnowledgePointsData
   中的列表插入数据库。逻辑极简，无任何可失败的 IO 操作。

3. AppViewModel.kt — init 块中调用 repo.seedKnowledgePoints()（去掉 app
   参数），其余不变，仍在 Dispatchers.IO 上运行。

4. AppDatabase.kt — version 从 1 升到 2，触发 fallbackToDestructiveMigration
   在已安装设备上重建数据库，确保所有用户都能收到完整知识点数据。

效果：
- 新安装：直接获得全部初中物理知识点
- 升级安装：数据库重建后自动重新写入知识点（用户自定义知识点需重新添加）
- 不再依赖 assets/knowledge_points.json（文件保留但不再使用）
