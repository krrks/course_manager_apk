# Release Notes — v1.5.0 / 更新说明 — v1.5.0

## Bug Fixes / 修复内容

### 1. Data loss on exit / 退出后数据丢失
**EN:** Fixed data being lost when the app is closed. The root cause was that `save()` in `AppViewModel` was wrapped inside `viewModelScope.launch { }` — the coroutine was scheduled but the process was killed before it could execute. `prefs.edit().apply()` is now called directly on the main thread, so data is written immediately on every change.

**中文：** 修复退出 App 后添加的数据消失的问题。根本原因是 `AppViewModel.save()` 被 `viewModelScope.launch` 包裹，进程结束前协程来不及执行。现在直接在主线程调用 `prefs.edit().apply()`，每次数据变更后立即持久化。

**File changed / 修改文件：**
- `app/src/main/java/com/school/manager/viewmodel/AppViewModel.kt`

---

### 2. Time wheel: scroll then click required / 时间滚轮滚动后需二次点击
**EN:** Fixed the time picker wheel requiring an extra tap after scrolling to confirm the selection. The wheel now automatically snaps to and selects the nearest item when scrolling stops. Tapping an item directly also works.

**中文：** 修复时间滚轮滚动后还需再次点击才能选中时间的问题。现在滚动停止时自动吸附并选中最近的时间项，无需二次点击。直接点击列表项同样可以立即选中。

**File changed / 修改文件：**
- `app/src/main/java/com/school/manager/ui/screens/ScheduleScreen.kt` — `TimeWheelPicker` / `WheelColumn`

---

### 3. FAB menu pops up at screen top on non-schedule pages / 其他页面 FAB 在屏幕顶部弹出
**EN:** Fixed the speed-dial FAB on non-schedule screens (Attendance, Classes, Teachers, Students, Stats, Settings) expanding at the wrong position (top of the screen) instead of above the FAB button. All screens now use `ScreenSpeedDialFab` placed inside `Scaffold`'s `floatingActionButton` slot, which correctly anchors the menu to the FAB.

**中文：** 修复除课表页面以外的其他页面（上课记录、班级、教师、学生、课时统计、设置）中速拨 FAB 点击后菜单在屏幕顶部弹出而非在 FAB 上方展开的问题。所有页面现在均将 `ScreenSpeedDialFab` 放置于 `Scaffold` 的 `floatingActionButton` 插槽中，菜单位置正确锚定在 FAB 按钮上方。

**File changed / 修改文件：**
- `app/src/main/java/com/school/manager/ui/screens/ScheduleScreen.kt`

---

### 4. Exported image: time range missing content after 19:00 / 导出图片时间范围缺少 19:00 以后的内容
**EN:** Fixed the exported JPG schedule image cutting off all courses scheduled after 19:00. The bitmap renderer previously used the fixed `PERIOD_TIMES` list (8 discrete time slots ending at 19:00) as its time axis. It now uses a continuous hour-by-hour axis from `CAL_START_HOUR` (08:00) to `CAL_END_HOUR` (22:00), matching the in-app calendar view. Course blocks are positioned by their actual start/end minutes relative to 08:00.

**中文：** 修复导出课表图片中 19:00 之后的课程内容缺失的问题。之前图片渲染使用固定的 `PERIOD_TIMES` 列表（8个离散时间段，最后一段为 19:00）作为时间轴，导致晚间课程被截断。现在改为与应用内日历视图一致的连续时间轴（`CAL_START_HOUR` 08:00 至 `CAL_END_HOUR` 22:00），课程块按实际起止分钟数相对 08:00 定位。

**File changed / 修改文件：**
- `app/src/main/java/com/school/manager/ui/screens/ScheduleScreen.kt` — `renderScheduleBitmap`

---

### 5. Exported image: course blocks missing start/end time / 导出图片课程块缺少起止时间
**EN:** Fixed exported JPG course blocks not showing the specific start and end time. Each block now renders a `"HH:MM–HH:MM"` time label (e.g. `08:00–08:45`) below the subject name and class name.

**中文：** 修复导出课表图片中课程块上未显示具体起止时间的问题。每个课程块现在在科目名和班级名下方额外绘制 `"HH:MM–HH:MM"` 格式的时间标签（如 `08:00–08:45`）。

**File changed / 修改文件：**
- `app/src/main/java/com/school/manager/ui/screens/ScheduleScreen.kt` — `renderScheduleBitmap`

---

## Files in this patch / 本次补丁包含的文件

```
app/src/main/java/com/school/manager/viewmodel/AppViewModel.kt
app/src/main/java/com/school/manager/ui/screens/ScheduleScreen.kt
zip_update/release_note.md
```
