#!build
# 修复知识点初始化：删除旧版兼容，数据库重置为版本1

- AppDatabase: 版本从2降至1，删除MIGRATION_1_2（title字段已是基础schema的一部分）
- AppRepository: 删除所有旧版SharedPreferences兼容代码
- 新安装时knowledge_points.json内容将正确显示在知识点管理页面
- fallbackToDestructiveMigration确保升级用户自动重建数据库
