# 🏫 智慧课务管理系统

A school schedule and attendance management app for Android, built with **Jetpack Compose** and **Fluent Design** aesthetics.

---

## ✨ Features

| Module | Description |
|--------|-------------|
| 📅 课表 | 5-day timetable grid with subject colors and teacher/student filters |
| 📋 上课记录 | Attendance list + month calendar view with completion tracking |
| 🏫 班级管理 | Class cards with grade, headteacher, subject, and enrollment progress |
| 👩‍🏫 教师管理 | Teacher profiles with avatar, gender, phone, and class assignments |
| 🎒 学生管理 | Student table with grade, gender, and multi-class support |
| 📚 科目管理 | Subject cards with color coding and teacher assignment |
| 📊 课时统计 | Stats by teacher / grade / student |
| 📤 导出 | Export full state or filtered schedule/attendance as JSON |

---

## 🎨 Design

- **Fluent Design** principles: depth, motion, rounded surfaces
- **Material 3** (Material You) with dynamic color on Android 12+
- Custom blue palette: `#1A56DB` primary
- Side navigation drawer (Etar Calendar style)
- `ExtendedFloatingActionButton` for all add actions
- Smooth slide/fade transitions between screens

---

## 🏗️ Building

### Requirements

- Android Studio Ladybug (2024.2+) or newer
- JDK 17
- Android SDK 35
- Min API 26 (Android 8.0)

### Build with Android Studio

1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Run on a device or emulator (API 26+)

### Build via command line

```bash
chmod +x gradlew
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

---

## 🚀 GitHub Actions

### Auto build (`build.yml`)

Triggers on every push. Detects a `update_<timestamp>.zip` in the repo root and applies it before building, so you can ship source patches without committing individual files.

- Uploads the debug APK as a workflow artifact on every successful build
- Skips the build if only `.github/` files changed

### Manual release (`release.yml`)

Trigger manually with a version tag (e.g. `v1.2.0`) to build both debug and release APKs and publish a GitHub Release.

```bash
# Trigger via git tag
git tag v1.2.0
git push origin v1.2.0

# Or trigger manually in GitHub → Actions → Manual Release APK
```

---

## 📦 Applying a Patch ZIP

The `build.yml` workflow automatically detects `update_<timestamp>.zip` files in the repo root and unpacks them into the source tree before compiling. The zip must preserve the full relative path from the repo root, e.g.:

```
/app/src/main/java/com/school/manager/viewmodel/AppViewModel.kt
/app/src/main/res/values/themes.xml
/zip_update/release_note.md
/README.md
```

> **Do not include `build.yml` inside the zip** — deliver it as a standalone file if it needs updating.

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| State | ViewModel + StateFlow (in-memory + SharedPreferences) |
| Image loading | Coil (AsyncImage) |
| Serialization | Gson |
| Build | Gradle 8.7 + AGP 8.5 |

---

## 📱 Minimum Requirements

- Android 8.0 (API 26+)
- ~20 MB storage

---

## 🗂️ Project Structure

```
app/src/main/java/com/school/manager/
├── data/
│   └── Models.kt              # Domain models: Teacher, SchoolClass, Student, …
├── ui/
│   ├── components/
│   │   └── CommonComponents.kt
│   ├── screens/
│   │   ├── AttendanceScreen.kt
│   │   ├── ClassesScreen.kt
│   │   ├── ExportScreen.kt
│   │   ├── ScheduleScreen.kt
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
├── viewmodel/
│   └── AppViewModel.kt        # All CRUD + state persistence
├── MainActivity.kt
└── Navigation.kt
```

---
