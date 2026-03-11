## Bug Fix: Compilation errors

- **AppViewModel**: Added missing `clearScheduleFilter()`, `mergeImport()`, `exportFullBackupZip()`, `importFullBackupZip()`. Fixed `importMerge` expression body → block body (was causing "Returns are prohibited for functions with expression body" error).
- **FluentComponentAliases.kt** (new file): Added `FluentTextField`, `DropdownField`, `AutocompleteTextField` composables used by `ClassesScreen` but missing from `CommonComponents.kt`.
