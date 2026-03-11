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

Triggers on every push. Detects `update_<timestamp>.zip` inside `zip_update/` and applies it to the repo before building.

- APKs are uploaded as workflow artifacts and attached to a GitHub Release on every successful build
- Skips the build if only `.github/` files changed or the zip was already released

### Manual release (`release.yml`)

Trigger manually with a version tag (e.g. `v1.2.0`) to build both debug and release APKs and publish a GitHub Release.

---

## 📦 Applying a Patch ZIP

The workflow looks for `zip_update/update_<timestamp>.zip`. The zip **must use paths relative to the repo root** — there must be **no wrapper/outer directory** inside the zip. The workflow always extracts directly from the zip root to the repo root.

### ✅ Correct zip structure

```
app/src/main/java/com/school/manager/viewmodel/AppViewModel.kt
app/src/main/java/com/school/manager/ui/screens/ClassesScreen.kt
zip_update/release_note.md
README.md
```

When you run `unzip -l update_xxx.zip` you should see paths starting with `app/`, `zip_update/`, etc. — **not** a single top-level folder wrapping everything.

### ❌ Wrong zip structure (causes files to land in wrong place)

```
some_folder/app/src/main/java/...   ← extra wrapper dir = broken
```

### How to create a correct zip (run from repo root)

```bash
zip -r zip_update/update_$(date +%s).zip \
  app/src/main/java/com/school/manager/viewmodel/AppViewModel.kt \
  app/src/main/java/com/school/manager/ui/screens/ClassesScreen.kt \
  zip_update/release_note.md
```

> **Do not include `.github/build.yml` inside the zip** — deliver it as a standalone file if it needs updating.

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
course_manager_apk/
├── .github/
│   └── workflows/
│       ├── build.yml
│       └── release.yml
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/
│           │   └── com/school/manager/
│           │       ├── MainActivity.kt
│           │       ├── Navigation.kt
│           │       ├── data/
│           │       │   └── Models.kt
│           │       ├── ui/
│           │       │   ├── components/
│           │       │   │   ├── CommonComponents.kt
│           │       │   │   └── FluentComponentAliases.kt
│           │       │   ├── screens/
│           │       │   │   ├── AttendanceScreen.kt
│           │       │   │   ├── ClassesScreen.kt
│           │       │   │   ├── ExportScreen.kt
│           │       │   │   ├── ScheduleScreen.kt
│           │       │   │   ├── StatsScreen.kt
│           │       │   │   ├── StudentsScreen.kt
│           │       │   │   ├── SubjectsScreen.kt
│           │       │   │   └── TeachersScreen.kt
│           │       │   └── theme/
│           │       │       ├── Color.kt
│           │       │       ├── Theme.kt
│           │       │       └── Type.kt
│           │       ├── util/
│           │       │   └── AvatarUtil.kt
│           │       └── viewmodel/
│           │           └── AppViewModel.kt
│           └── res/
│               ├── drawable/
│               │   └── ic_launcher_foreground.xml
│               ├── mipmap-anydpi-v26/
│               │   ├── ic_launcher.xml
│               │   └── ic_launcher_round.xml
│               └── values/
│                   ├── colors.xml ← dir in zip: ./app/src/main/res/values/colors.xml
│                   ├── strings.xml
│                   └── themes.xml
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── zip_update/
│   ├── file_list.md               ← full repo file list, auto-updated by workflow
│   ├── file_list_befor_update.md
│   ├── note.md                    ← last patch summary, auto-updated by workflow
│   ├── release_note.md            ← human-written change description, included in zip
│   └── update_1773220799.zip      ← uploaded zip file
├── .gitignore
├── README.md                      ← dir in zip: ./README.md
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── settings.gradle.kts
```

---

