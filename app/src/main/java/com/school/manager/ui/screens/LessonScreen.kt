package com.school.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import com.school.manager.data.*
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import java.time.LocalDate
import com.school.manager.viewmodel.AppViewModel

@Composable
fun LessonScreen(vm: AppViewModel, onOpenDrawer: () -> Unit = {}) {
    val state   by vm.state.collectAsState()
    var view    by remember { mutableStateOf("week") }
    var calDate by remember { mutableStateOf(LocalDate.now()) }

    // Zoom state (week + day views only)
    var zoomIdx   by remember { mutableIntStateOf(ZOOM_DEFAULT_IDX) }
    val dpPerHour = ZOOM_LEVELS[zoomIdx]

    // Filters
    var fClass   by remember { mutableLongStateOf(0L) }
    var fStatus  by remember { mutableStateOf("") }
    var fTeacher by remember { mutableLongStateOf(0L) }
    var fStudent by remember { mutableLongStateOf(0L) }
    var fSubjects  by remember { mutableStateOf(emptySet<Long>()) }
    var fDayOfWeek by remember { mutableStateOf(emptySet<Int>()) }
    var fFromDate  by remember { mutableStateOf("") }
    var fToDate    by remember { mutableStateOf("") }

    // Multi-select (list view only)
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds     by remember { mutableStateOf(emptySet<Long>()) }
    var showBatchAction by remember { mutableStateOf(false) }

    var showFilterSheet by remember { mutableStateOf(false) }
    val hasActiveFilters = fClass != 0L || fStatus.isNotEmpty() || fTeacher != 0L ||
                          fStudent != 0L || fSubjects.isNotEmpty() || fDayOfWeek.isNotEmpty() ||
                          fFromDate.isNotBlank() || fToDate.isNotBlank()

    var viewing      by remember { mutableStateOf<Lesson?>(null) }
    var editing      by remember { mutableStateOf<Lesson?>(null) }
    var showAdd      by remember { mutableStateOf(false) }
    var showBatchGen by remember { mutableStateOf(false) }
    var batchModCls  by remember { mutableLongStateOf(0L) }
    var batchDelCls  by remember { mutableLongStateOf(0L) }

    val progressMap: Map<Long, Pair<Int, Int>> = remember(state.lessons) {
        state.lessons.groupBy { it.classId }.mapValues { (_, ls) ->
            ls.count { it.status == "completed" } to ls.size
        }
    }

    fun clearAllFilters() {
        fClass = 0L; fStatus = ""; fTeacher = 0L; fStudent = 0L
        fSubjects = emptySet(); fDayOfWeek = emptySet()
        fFromDate = ""; fToDate = ""
    }

    val filtered = state.lessons.filter { l ->
        (fClass == 0L      || l.classId == fClass) &&
        (fStatus.isBlank() || l.status  == fStatus) &&
        (fTeacher == 0L    || l.effectiveTeacherId(state.classes) == fTeacher) &&
        (fStudent == 0L    || run {
            val s = state.students.find { it.id == fStudent }
            s != null && s.classIds.contains(l.classId)
        }) &&
        (fSubjects.isEmpty() || run {
            val subId = state.classes.find { it.id == l.classId }?.subjectId
            subId != null && subId in fSubjects
        }) &&
        (fDayOfWeek.isEmpty() || runCatching {
            LocalDate.parse(l.date).dayOfWeek.value in fDayOfWeek
        }.getOrDefault(false)) &&
        (fFromDate.isBlank() || l.date >= fFromDate) &&
        (fToDate.isBlank()   || l.date <= fToDate)
    }.sortedWith(compareBy({ it.date }, { it.startTime }))

    Scaffold(
        floatingActionButton = {
            if (!isSelectionMode) {
                ScreenSpeedDialFab(
                    addLabel     = "添加单节课",
                    addIcon      = Icons.Default.Add,
                    onAdd        = { showAdd = true },
                    onOpenDrawer = onOpenDrawer,
                    extraItems   = {
                        // Zoom controls — only in week / day views
                        if (view == "week" || view == "day") {
                            SpeedDialItem(
                                label    = "放大",
                                icon     = Icons.Default.ZoomIn,
                                color    = FluentBlue,
                                selected = zoomIdx == ZOOM_LEVELS.lastIndex
                            ) { zoomIdx = (zoomIdx + 1).coerceAtMost(ZOOM_LEVELS.lastIndex) }
                            SpeedDialItem(
                                label    = "缩小",
                                icon     = Icons.Default.ZoomOut,
                                color    = FluentMuted,
                                selected = zoomIdx == 0
                            ) { zoomIdx = (zoomIdx - 1).coerceAtLeast(0) }
                        }
                        SpeedDialItem(
                            label    = "筛选条件",
                            icon     = Icons.Default.FilterList,
                            color    = if (hasActiveFilters) FluentOrange else FluentMuted,
                            selected = hasActiveFilters
                        ) { showFilterSheet = true }
                        SpeedDialItem("批量生成课次", Icons.Default.AutoAwesome, FluentGreen) {
                            showBatchGen = true
                        }
                    }
                )
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(top = inner.calculateTopPadding(), bottom = inner.calculateBottomPadding())
        ) {
            when (view) {
                "week"  -> WeekView(filtered, calDate, { calDate = it }, state, progressMap,
                               view, { view = it }, dpPerHour) { viewing = it }
                "month" -> MonthView(filtered, calDate, { calDate = it }, state, progressMap,
                               view, { view = it }) { viewing = it }
                "day"   -> DayView(filtered, calDate, { calDate = it }, state, progressMap,
                               view, { view = it }, dpPerHour) { viewing = it }
                "list"  -> ListView(
                               lessons              = filtered,
                               state                = state,
                               progressMap          = progressMap,
                               currentView          = view,
                               onViewChange         = {
                                   view = it
                                   isSelectionMode = false
                                   selectedIds = emptySet()
                               },
                               onLessonClick        = { viewing = it },
                               onBatchModify        = { batchModCls = it },
                               onBatchDelete        = { batchDelCls = it },
                               isSelectionMode      = isSelectionMode,
                               selectedIds          = selectedIds,
                               onSelectionChange    = { selectedIds = it },
                               onEnterSelectionMode = { isSelectionMode = true },
                               onExitSelectionMode  = {
                                   isSelectionMode = false
                                   selectedIds = emptySet()
                               },
                               onBatchAction        = { showBatchAction = true }
                           )
            }
        }
    }

    // ── Filter bottom sheet ───────────────────────────────────────────────
    if (showFilterSheet) {
        FilterBottomSheet(
            state             = state,
            fClass            = fClass,     onClassChange      = { fClass   = it },
            fStatus           = fStatus,    onStatusChange     = { fStatus  = it },
            fTeacher          = fTeacher,   onTeacherChange    = { fTeacher = it },
            fStudent          = fStudent,   onStudentChange    = { fStudent = it },
            fSubjects         = fSubjects,  onSubjectsChange   = { fSubjects  = it },
            fDayOfWeek        = fDayOfWeek, onDayOfWeekChange  = { fDayOfWeek = it },
            fFromDate         = fFromDate,  onFromDateChange   = { fFromDate  = it },
            fToDate           = fToDate,    onToDateChange     = { fToDate    = it },
            hasActive         = hasActiveFilters,
            onClearAll        = { clearAllFilters() },
            onDismiss         = { showFilterSheet = false }
        )
    }

    // ── Dialogs ───────────────────────────────────────────────────────────
    viewing?.let { l ->
        LessonDetailDialog(l, state, vm, progressMap,
            onDismiss = { viewing = null },
            onEdit    = { editing = l; viewing = null },
            onDelete  = { vm.deleteLesson(l.id); viewing = null }
        )
    }
    editing?.let { l ->
        LessonFormDialog("编辑课次", l, state, vm,
            onDismiss = { editing = null },
            onSave    = { updated -> vm.updateLesson(updated, markModified = true); editing = null }
        )
    }
    if (showAdd) {
        LessonFormDialog("添加课次", null, state, vm,
            onDismiss = { showAdd = false },
            onSave    = { l ->
                vm.addLesson(l.classId, l.date, l.startTime, l.endTime,
                    l.status, l.topic, l.notes, l.attendees,
                    teacherIdOverride = l.teacherIdOverride)
                showAdd = false
            }
        )
    }
    if (showBatchGen) BatchGenerateDialog(state, vm, onDismiss = { showBatchGen = false })
    if (batchModCls > 0L) BatchModifyDialog(batchModCls, state, vm, onDismiss = { batchModCls = 0L })
    if (batchDelCls > 0L) BatchDeleteDialog(batchDelCls, state, vm, onDismiss = { batchDelCls = 0L })

    if (showBatchAction && selectedIds.isNotEmpty()) {
        BatchActionDialog(
            selectedIds = selectedIds,
            state       = state,
            vm          = vm,
            onDismiss   = {
                showBatchAction = false
                isSelectionMode = false
                selectedIds     = emptySet()
            }
        )
    }
}
