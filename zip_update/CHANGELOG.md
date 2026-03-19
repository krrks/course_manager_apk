#!build
# UI 功能增强：教师覆盖、班级添加、筛选器扩展

## 功能变更

### 1. 班级页面增加添加按钮
- ClassesScreen：ScreenSpeedDialFab 增加 addLabel="添加班级" + onAdd，原来只有导航无法新增

### 2 & 5. 课次添加/编辑允许修改教师
- Lesson 模型新增 teacherIdOverride: Long?（null 表示使用班级默认班主任）
- LessonFormDialog（添加和编辑共用）增加教师下拉选择器
  - 默认值自动填入所选班级的班主任
  - 切换班级时自动重置为新班级的班主任
  - 仅当选择的教师与班级默认不同时才存储 override
- LessonDetailDialog 展示实际生效教师，override 时显示"已覆盖"提示
- AppViewModel.addLesson 增加 teacherIdOverride 参数
- Lesson.effectiveTeacherId() 扩展函数：优先 override，降级到 class.headTeacherId

### 3. 移除科目详情里的主讲教师
- SubjectsScreen：SubjectDetailDialog 删除"主讲教师"行
- SubjectRow 卡片同步移除"主讲：xxx"副标题（保留 SubjectFormDialog 中的教师字段）

### 4. 批量生成课次增加教师和跳过日期选择器
- BatchGenerateDialog 增加教师下拉（默认为班级班主任，可覆盖）
- 跳过日期从文本框改为日期选择器（"添加跳过日期"按钮 → DatePickerDialog）
- 已选跳过日期以 InputChip 展示，可点 × 单个删除
- AppViewModel.batchGenerateLessons 增加 teacherIdOverride 参数

### 6. 日历视图筛选器增加教师和学生选择器
- LessonScreen 过滤行增加"全部教师"和"全部学生"两个 DropdownFilterChip
- 教师筛选：按 lesson.effectiveTeacherId 过滤
- 学生筛选：显示该学生所属班级的所有课次（按 student.classIds 过滤）

### 数据层
- Lesson.teacherIdOverride 字段同步到 LessonEntity、Mappers、AppViewModel 的 Gson 解析
- AppDatabase 版本升至 2（fallbackToDestructiveMigration，首次启动加载示例数据）
