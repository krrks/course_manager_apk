## Fix deprecation warnings & UI: remove top title bar

### 1. Deprecation warning fixes
- **CommonComponents.kt**: `.menuAnchor()` → `.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)` (FormDropdown read-only field)
- **FluentComponentAliases.kt**: `.menuAnchor()` → `.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)` (AutocompleteTextField editable field)
- **ExportScreen.kt**: `Icons.Outlined.EventNote` → `Icons.AutoMirrored.Outlined.EventNote`
- **ScheduleScreen.kt**: `Icons.Filled.ViewList` → `Icons.AutoMirrored.Filled.ViewList`
- **ScheduleScreen.kt**: `Divider(...)` in CalendarGrid → `HorizontalDivider(...)`

### 2. UI: Remove top title bar, expose navigation via FAB
- **MainActivity.kt**: Removed `TopAppBar` from Scaffold; content now fills screen from the very top.
- **ScheduleScreen**: CalendarGrid day-header row (周一–周日) now sits at the top of the screen with no bar above it.
- **ScheduleScreen FAB**: Added "导航菜单" speed-dial item that opens the side navigation drawer.
- **Other screens**: A persistent menu `FloatingActionButton` (bottom-start) opens the navigation drawer, replacing the removed hamburger icon.
