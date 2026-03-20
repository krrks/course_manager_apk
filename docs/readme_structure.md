## Project Structure

```
app/src/main/java/com/school/manager/
├── MainActivity.kt
├── Navigation.kt
├── data/
│   ├── Models.kt                    # domain models + sample data
│   ├── db/
│   │   ├── AppDatabase.kt           # Room database singleton
│   │   ├── Daos.kt                  # DAO interfaces (Flow + suspend CRUD)
│   │   ├── Entities.kt              # Room entities with FK annotations
│   │   └── Mappers.kt               # entity ↔ domain model converters
│   └── repository/
│       └── AppRepository.kt         # single source of truth; merges 5 flows → AppState
├── ui/
│   ├── components/
│   │   ├── AvatarComponents.kt
│   │   ├── CommonComponents.kt
│   │   ├── FluentComponentAliases.kt
│   │   └── SpeedDialFab.kt
│   ├── screens/
│   │   ├── ClassesScreen.kt
│   │   ├── ExportScreen.kt
│   │   ├── ExportImportDialog.kt
│   │   ├── LessonBatchActionDialog.kt
│   │   ├── LessonBatchDialogs.kt
│   │   ├── LessonDialogs.kt
│   │   ├── LessonFilterSheet.kt
│   │   ├── LessonListView.kt
│   │   ├── LessonScreen.kt
│   │   ├── LessonTimeHelpers.kt
│   │   ├── LessonViews.kt
│   │   ├── StatsScreen.kt
│   │   ├── StudentsScreen.kt
│   │   ├── SubjectsScreen.kt
│   │   └── TeachersScreen.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── util/
│   └── AvatarUtil.kt
└── viewmodel/
    ├── AppViewModel.kt              # delegates all persistence to AppRepository
    ├── BackupManager.kt             # ZIP backup/restore logic
    └── GsonModels.kt                # Gson transfer models for export/import boundary

docs/                                # README source files — auto-assembled at build time
├── readme_header.md                 # Features, Tech Stack, Data Storage
├── readme_build.md                  # Building, CI/CD, Patch delivery, apply.sh
├── readme_structure.md              # Project Structure (this file)
└── readme_ai_rules.md               # AI Rules
```

---
