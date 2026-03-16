# 🏫 智慧课务管理系统

A school schedule and attendance management app for Android, built with **Jetpack Compose** and **Fluent Design** aesthetics.

---

## ✨ Features

| Module | Description |
|--------|-------------|
| 📅 课表 | 5-day timetable grid with subject colors, teacher/student filters, and JPG export |
| 📋 上课记录 | Attendance list + week/month/day calendar views with completion tracking |
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
- Speed-dial FAB for all add/filter actions
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

Triggers on every push. Detects `zip_update/update_<timestamp>.zip` and applies it to the repo before building.

- APKs are uploaded as workflow artifacts and attached to a GitHub Release on every successful build
- Skips the build if only `.github/` files changed or the zip was already released
- **⚠️ Emits a prominent warning** in the build log if no `update_*.zip` is found — the build continues normally using current source

### Manual release (`release.yml`)

Trigger manually with a version tag (e.g. `v1.2.0`) to build both debug and release APKs and publish a GitHub Release.

---

## 📦 Applying a Patch ZIP

The workflow looks for `zip_update/update_<timestamp>.zip`. The zip **must use paths relative to the repo root** — there must be **no wrapper/outer directory** inside the zip.

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

## 🛠️ patch.sh — 自定义 patch 脚本

除了替换文件，zip 中还可以包含一个 **`zip_update/patch.sh`** 脚本，在解压完成之后、编译之前自动执行，方便做文件级别无法覆盖的变更操作。

### 执行时机

```
解压 zip → 执行 patch.sh → stamp versionName → 编译 APK → commit & push → GitHub Release
```

### 使用规则

| 规则 | 说明 |
|------|------|
| 路径固定 | 必须放在 zip 内的 `zip_update/patch.sh` |
| 工作目录 | repo root（执行时 `pwd` 即为仓库根目录） |
| 解释器 | `bash` |
| 退出码 | 非 `0` 时 workflow 立即报错终止，保护构建不被破坏的脚本污染 |
| 自动清理 | 执行完后，`patch.sh` 随 `*.zip` 一起被删除，不会留在仓库里 |
| 可选 | zip 中不包含 `patch.sh` 时，此步骤静默跳过，不报错 |

### 示例 patch.sh

```bash
#!/usr/bin/env bash
set -e   # 任何命令失败立即退出（触发 workflow 报错）

# 1. 删除已废弃的文件
rm -f app/src/main/java/com/school/manager/ui/screens/OldScreen.kt
echo "✅ Removed OldScreen.kt"

# 2. 批量替换包名中的旧字符串
find app/src -name '*.kt' -exec sed -i 's/OldComponentName/NewComponentName/g' {} +
echo "✅ Renamed OldComponentName → NewComponentName"

# 3. 生成或更新某个资源文件
echo '<?xml version="1.0" encoding="utf-8"?><resources><string name="app_name">课务管理</string></resources>' \
  > app/src/main/res/values/strings.xml
echo "✅ Updated strings.xml"
```

将此脚本放进 zip：

```bash
zip -r zip_update/update_$(date +%s).zip \
  app/src/main/java/com/school/manager/ui/screens/NewScreen.kt \
  zip_update/patch.sh \
  zip_update/release_note.md
```

---

## 🔔 No-ZIP Warning

如果推送时 `zip_update/` 目录下**没有** `update_*.zip` 文件，`build.yml` 会在日志中输出一个显眼的警告块：

```
##################################################
#                                                #
#   ⚠️  WARNING: No update_*.zip found in       #
#   zip_update/                                  #
#                                                #
#   Build will proceed using current source.     #
#   If you intended to apply a patch, make sure  #
#   the zip is named  update_<timestamp>.zip     #
#   and placed in the zip_update/ directory.     #
#                                                #
##################################################
```

构建仍会继续（使用仓库当前源码）。这仅是一个提醒，不会导致失败。

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
│       ├── release.yml
│       └── update_apk_key.yml
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/school/manager/
│           ├── MainActivity.kt
│           ├── Navigation.kt
│           ├── data/Models.kt
│           ├── ui/
│           │   ├── components/
│           │   │   ├── CommonComponents.kt
│           │   │   └── FluentComponentAliases.kt
│           │   ├── screens/
│           │   │   ├── AttendanceScreen.kt
│           │   │   ├── ClassesScreen.kt
│           │   │   ├── ExportScreen.kt
│           │   │   ├── ScheduleScreen.kt
│           │   │   ├── StatsScreen.kt
│           │   │   ├── StudentsScreen.kt
│           │   │   ├── SubjectsScreen.kt
│           │   │   └── TeachersScreen.kt
│           │   └── theme/
│           │       ├── Color.kt
│           │       ├── Theme.kt
│           │       └── Type.kt
│           ├── util/AvatarUtil.kt
│           └── viewmodel/AppViewModel.kt
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── keys/
│   ├── release.jks                ← signing keystore (generated by update_apk_key.yml)
│   └── keystore.properties        ← store/key passwords
├── zip_update/
│   ├── file_list.md               ← full repo snapshot, auto-updated after each patch
│   ├── file_list_befor_update.md
│   ├── note.md                    ← last patch summary
│   ├── patch.sh                   ← (optional) custom post-extract script, included in zip
│   ├── release_note.md            ← human-written change description, included in zip
│   └── update_<timestamp>.zip     ← patch zip, deleted after apply
├── .gitignore
├── README.md
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── settings.gradle.kts
```
