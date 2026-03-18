#!build
# 迁移存储层：SharedPreferences JSON → Room SQLite

## 变更概述

将数据持久化从「整包 JSON 写入 SharedPreferences」迁移到 Room 数据库，解决所有孤儿数据 bug。

## 新增文件

- `data/db/Entities.kt` — 6 个 Room 实体，带完整 FK 注解
- `data/db/Daos.kt` — 6 个 DAO，Flow + suspend CRUD
- `data/db/AppDatabase.kt` — Room 数据库单例（school_manager.db）
- `data/db/Mappers.kt` — 实体 ↔ 领域模型双向转换
- `data/repository/AppRepository.kt` — 统一数据源，合并 6 个 Flow 为 AppState

## 修改文件

- `viewmodel/AppViewModel.kt` — 使用 Repository，公共 API 完全不变，所有 UI 无需改动
- `app/build.gradle.kts` — 添加 `kotlin-kapt` 插件 + Room 依赖
- `gradle/libs.versions.toml` — 添加 room 版本号及三个库条目

## 解决的 Bug（数据库层面自动处理，无需手写级联逻辑）

| FK 约束 | 效果 |
|---------|------|
| `schedule.classId → classes ON DELETE CASCADE` | 删除班级自动删除其所有课表 |
| `attendance.classId → classes ON DELETE CASCADE` | 删除班级自动删除其所有记录 |
| `schedule.subjectId → subjects ON DELETE SET_NULL` | 删除科目自动置空课表科目引用 |
| `attendance.subjectId → subjects ON DELETE SET_NULL` | 删除科目自动置空记录科目引用 |
| `classes.subjectId → subjects ON DELETE SET_NULL` | 删除科目自动置空班级科目引用 |
| `schedule.teacherId → teachers ON DELETE SET_NULL` | 删除教师自动置空课表教师引用 |
| `attendance.teacherId → teachers ON DELETE SET_NULL` | 删除教师自动置空记录教师引用 |
| `classes.headTeacherId → teachers ON DELETE SET_NULL` | 删除教师自动置空班主任引用 |

## 向后兼容

- 首次启动自动从旧 SharedPreferences 读取并迁移数据到 Room，完成后删除旧 key
- JSON / ZIP 导出格式与之前完全相同，导入逻辑兼容旧备份文件
