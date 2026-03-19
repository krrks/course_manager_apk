#!build
# UI 重构：日历页顶部空间释放，视图切换内嵌标题栏，筛选器移至 FAB BottomSheet

## 变更内容

### 移除顶部两行（约 100dp 空间释放）
- 删除视图切换 Tab 行（周/月/日/列表 FilterChip）
- 删除筛选 chips 行（班级/状态/教师/学生）
- 日历可视区域相应扩大

### 视图切换：内嵌至各视图蓝色标题栏
- WeekView / MonthView / DayView / ListView 的蓝色标题栏右侧统一新增 4 个紧凑图标按钮
- 图标：ViewWeek / CalendarMonth / Today / ViewList
- 当前视图图标高亮白色，其余半透明，一眼可识当前状态
- ListView 原无标题栏，新增蓝色标题栏（"课次列表"）以保持一致性
- 标题文字改为 weight(1f) + TextAlign.Center，与左右箭头及视图图标自然分布

### 筛选器：移入 FAB SpeedDial → ModalBottomSheet
- FAB SpeedDial 新增"筛选条件"项（FilterList 图标）
- 有筛选条件生效时该项高亮橙色（FluentOrange），无筛选时为灰色
- 点击后展开 ModalBottomSheet，内含班级 / 状态 / 教师 / 学生四组 FilterChip
- 顶部提供"清除全部"按钮，仅在有活跃筛选时显示
- SpeedDial 展开项顺序：筛选条件 → 批量生成课次 → 添加单节课 → 导航菜单

### 仅修改文件
- `app/src/main/java/com/school/manager/ui/screens/LessonScreen.kt`
- 数据层、ViewModel、CommonComponents 均无变动
