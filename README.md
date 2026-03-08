# 🏫 智慧课务管理系统

A school schedule and attendance management app for Android, built with **Jetpack Compose** and **Fluent Design** aesthetics, inspired by the [Etar Calendar](https://github.com/Etar-Group/Etar-Calendar) UI.

---

## ✨ Features

| Module | Description |
|--------|-------------|
| 📅 课表 | 5-day × 8-period timetable grid with subject colors |
| 📋 上课记录 | Attendance list + month calendar view |
| 🏫 班级管理 | Class cards with progress indicators |
| 👩‍🏫 教师管理 | Teacher list with subject assignments |
| 🎒 学生管理 | Student table with grade + multi-class support |
| 📚 科目管理 | Subject cards with color coding |
| 📊 课时统计 | Stats by teacher / grade / student with unified filters |

---

## 🎨 Design

- **Fluent Design** principles: depth, motion, rounded surfaces
- **Material 3** (Material You) with dynamic color on Android 12+
- Custom blue palette: `#1A56DB` primary
- `ModalNavigationDrawer` (Etar-style side navigation)
- `ExtendedFloatingActionButton` for all add actions
- Smooth slide animations between screens

---

## 🏗️ Building

### Requirements
- Android Studio Ladybug (2024.2+)
- JDK 17
- Android SDK 35

### Build with Android Studio
1. Open the project in Android Studio
2. Let Gradle sync complete
3. Run on a device or emulator (min API 26)

### Build via command line
```bash
chmod +x gradlew
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

---

## 🚀 GitHub Actions (Auto-Release)

The workflow at `.github/workflows/release.yml` automatically:

1. **Builds** both debug and release APKs on every tagged push
2. **Uploads** APKs as workflow artifacts (always)
3. **Creates a GitHub Release** with APKs attached (on `v*.*.*` tags)

### Trigger a release
```bash
git tag v1.0.0
git push origin v1.0.0
```

### Manual trigger
Go to **Actions → Build & Release APK → Run workflow**

---

## 📱 Minimum Requirements

- Android 8.0 (API 26+)
- ~20 MB storage

---

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Navigation**: Navigation Compose
- **State**: ViewModel + StateFlow (in-memory)
- **Build**: Gradle 8.7 + AGP 8.5
