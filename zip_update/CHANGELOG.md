#!build
# 修复班级编辑导致课程被删除 + 科目管理页 + 科目ID

## Bug 修复

**编辑班级不再删除课程表行程**
根本原因：所有 DAO 使用 `@Insert(onConflict = REPLACE)`，SQLite 的 REPLACE 策略
内部执行 DELETE + INSERT，触发了 `schedule.classId → classes.id ON DELETE CASCADE`，
导致每次"更新"班级时对应的所有排课行被级联删除。
修复方案：将每个 DAO 拆分为 `@Insert(IGNORE)` + `@Update` 两个方法，Repository
的 addXxx 调用 insert()，updateXxx 调用 update()，彻底避免级联副作用。
同样修复了更新科目/教师时意外 SET NULL 子行外键的同类问题。

## 新功能

**科目管理页面独立入口**
- 导航抽屉新增"科目管理"菜单项（MenuBook 图标）
- SubjectsScreen 改为列表布局，每行显示颜色条、名称、编号、主讲教师、排课/记录统计
- 详情对话框显示科目编号
- 编辑/添加表单：编号字段排在首位，颜色选择保留

**科目添加 code 字段（ID）**
- `Subject` domain model 新增 `code: String` 字段，与 Teacher/Class/Schedule/Attendance 保持一致
- `SubjectEntity` 新增 `code TEXT NOT NULL DEFAULT ''` 列
- Room 数据库版本 1 → 2，`MIGRATION_1_2` 通过 ALTER TABLE 平滑升级，无数据丢失
- `addSubject` 自动生成 `SBJ` 前缀编号；样本数据赋予 SBJ00001–SBJ00005

## 其它字段 ID 检查结果
Teacher(code)、SchoolClass(code)、Schedule(code)、Attendance(code) 均已有 code 字段，
Student 无 code 字段，但与其他实体关联方式不同（通过 studentNo 标识），不需要额外 code。
