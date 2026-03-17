# 🗂 仓库完整文件列表

> This file is generated for repo checking.
> It includes **all** files in the repo with no exclusions (except `.git/` internals and build outputs).

```
./.gitignore
./.github/workflows/build.yml
./.github/workflows/release.yml
./README.md
./app/build.gradle.kts
./app/proguard-rules.pro
./app/src/main/AndroidManifest.xml
./app/src/main/java/com/school/manager/MainActivity.kt
./app/src/main/java/com/school/manager/Navigation.kt
./app/src/main/java/com/school/manager/data/Models.kt
./app/src/main/java/com/school/manager/ui/components/CommonComponents.kt
./app/src/main/java/com/school/manager/ui/screens/AttendanceScreen.kt
./app/src/main/java/com/school/manager/ui/screens/ClassesScreen.kt
./app/src/main/java/com/school/manager/ui/screens/ExportScreen.kt
./app/src/main/java/com/school/manager/ui/screens/ScheduleScreen.kt
./app/src/main/java/com/school/manager/ui/screens/StatsScreen.kt
./app/src/main/java/com/school/manager/ui/screens/StudentsScreen.kt
./app/src/main/java/com/school/manager/ui/screens/SubjectsScreen.kt
./app/src/main/java/com/school/manager/ui/screens/TeachersScreen.kt
./app/src/main/java/com/school/manager/ui/theme/Color.kt
./app/src/main/java/com/school/manager/ui/theme/Theme.kt
./app/src/main/java/com/school/manager/ui/theme/Type.kt
./app/src/main/java/com/school/manager/util/AvatarUtil.kt
./app/src/main/java/com/school/manager/viewmodel/AppViewModel.kt
./app/src/main/res/drawable/ic_launcher_foreground.xml
./app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
./app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
./app/src/main/res/values/colors.xml
./app/src/main/res/values/strings.xml
./app/src/main/res/values/themes.xml
./build.gradle.kts
./gradle.properties
./gradle/libs.versions.toml
./gradle/wrapper/gradle-wrapper.jar
./gradle/wrapper/gradle-wrapper.properties
./gradlew
./gradlew.bat
./settings.gradle.kts
./zip_update/file_list.md
./zip_update/note.md
```

## 📋 Notes

- `build/` and `.gradle/` (build outputs & Gradle cache) are excluded — they are never committed
- `.git/` internals are excluded
- `zip_update/*.zip` patch files are deleted by the workflow after being applied and are not listed here
- `build.yml` auto-regenerates this file on every successful patch apply
