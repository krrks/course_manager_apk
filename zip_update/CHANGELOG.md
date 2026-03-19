#!build
# 合并课表与上课记录为统一 Lesson 模型

## 主要变更

### 数据层
- 删除 Schedule 表和 Attendance 表，合并为单一 Lesson 表
- SchoolClass 语义收紧：一个班级 = 一个固定时间槽教学单元（classId == groupId）
- Lesson.status：pending / completed / absent / cancelled / postponed
- Lesson.isModified：标记被单独修改过的课次，批量操作可选跳过
- 数据库重置为 v1，fallbackToDestructiveMigration（用户首次启动时自动加载示例数据）
- List<Lesson>.progressFor(classId) 扩展函数：返回 (已完成, 总计) 用于进度显示

### 视图层
- 删除 ScheduleScreen.kt 和 AttendanceScreen.kt
- 新增 LessonScreen.kt（周视图/月视图/日视图/列表视图四合一）
- 所有显示 Lesson 信息的地方均展示"已完成 X / Y 节"进度：
  - 列表视图：每个班级分组头部显示进度条 + 文字
  - 日视图：每个课次块内显示"已上 X/Y 节"
  - 周视图：每个课次块内显示"X/Y"
  - 详情弹窗：独立"上课进度"行 + 进度条
- 详情弹窗：快速改状态按钮（pending/completed/absent/cancelled/postponed）
- isModified=true 的课次在列表中显示"已改"标签

### 批量操作
- 批量生成：支持每周/连续每天/单次，可设排除日期
- 批量修改：起止日期范围，默认今天起，可选跳过 isModified 行，可选包含非 pending
- 批量删除：同样的范围控制 + 两步确认 + 显示目标数量

### 统计页
- StatsScreen 数据源从 attendance 改为 lessons（仅统计 status=completed）
- 课时改为实际 durationMinutes() 求和，不再使用固定 PERIOD_HOURS 常量

### 导出/导入
- exportScheduleJson / exportAttendanceJson → exportLessonsJson
- ZIP 格式不变（state.json + avatars/）
- GsonState 中 schedule/attendance 字段被 Gson 忽略，保持向后兼容

### 删除的文件
- app/src/main/java/com/school/manager/ui/screens/ScheduleScreen.kt
- app/src/main/java/com/school/manager/ui/screens/AttendanceScreen.kt
- AttendanceScreen_FormDialog_PATCH.kt（根目录）
