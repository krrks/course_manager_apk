#!build
# Plan A: 删除 Schedule/Attendance 的 subjectId，改为通过 classId JOIN 推导

## 核心变更

**方案 A：Schedule 和 Attendance 不再持有 subjectId，科目信息完全通过 classId → classes.subjectId 在展示层推导获取。**

### 改动文件

- `Models.kt` — `Schedule` / `Attendance` 删除 `subjectId` 字段；`SchoolClass` 删除冗余 `subject: String` 字段；删除 `resolvedSubjectName()` 三级回退函数；新增简洁的 `subjectName()` 扩展函数
- `Entities.kt` — `ScheduleEntity` / `AttendanceEntity` 删除 `subjectId` 列和对应 FK；`ClassEntity` 删除 `subject` 列
- `AppDatabase.kt` — 版本升至 3，新增 `MIGRATION_2_3`（重建三张表，保留所有业务数据）
- `Daos.kt` — 移除 subjectId 相关索引和查询
- `Mappers.kt` — 更新 mapper，无需再处理 subjectId null→0L 转换
- `AppRepository.kt` — `updateSchoolClass` 不再需要级联循环
- `AppViewModel.kt` — 删除 BUG-1/BUG-2 的 forEach 级联代码；`updateSubject` 删除 BUG-6 同步逻辑；`addSchedule` / `addAttendance` 签名保持兼容（subjectId 参数标注为 ignored）；JSON 导入兼容旧格式（subjectId 字段被忽略）
- `apply.sh` — 用 Python 精确替换四个 Screen 文件中所有 subjectId / resolvedSubjectName 引用

### 效果

- 修改班级科目 → 仅一次 `UPDATE classes SET subjectId=?`，课表和记录自动展示新科目，**零手动级联**
- 删除 `resolvedSubjectName()` 三级回退兜底逻辑
- 数据真相来源从 3 个减少到 1 个（`classes.subjectId`）
- 彻底消除 BUG-1 / BUG-2 / BUG-6 的根因
