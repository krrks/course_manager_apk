# School Schedule Manager

An Android app for managing school timetables and class attendance records, built with **Jetpack Compose** and **Material 3**.

---

## Features

| Module | Description |
|--------|-------------|
| Schedule | 5-day timetable grid with subject colors, teacher/class filters, and JPG export |
| Attendance | Class records with list, week, month, and day calendar views |
| Classes | Class cards with grade, head teacher, subject, and enrollment info |
| Teachers | Teacher profiles with avatar, gender, phone, and class assignments |
| Students | Student list with grade, gender, and multi-class support |
| Subjects | Subject cards with color coding and teacher assignment |
| Statistics | Lesson counts by teacher, grade, and student |
| Export | Export full state or filtered data as JSON or ZIP backup |

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| State | ViewModel + StateFlow |
| Image loading | Coil |
| Serialization | Gson |
| Build | Gradle 8.7 + AGP 8.5 |

---

## Building Locally

**Requirements:** Android Studio Ladybug 2024.2+, JDK 17, Android SDK 35, min API 26

```bash
# Debug build
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Release build
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

---

## CI/CD — GitHub Actions

### Auto build (`build.yml`)

Triggers on every push to any branch. The workflow:

1. Looks for a patch zip in `zip_update/patches/`
2. If found — extracts it, runs `zip_update/patches/apply.sh` (if present), then optionally compiles
3. If not found — proceeds with current source (warning printed in log)
4. On success with APK compilation — commits changes, tags a release, attaches APKs

**Skip conditions** (workflow exits early, no build):
- Only `.github/` files changed
- The same patch zip was already released

**Manual trigger** (`workflow_dispatch`): supports force-build and choosing debug/release variants.

---

## Updating `build.yml`

> **GitHub does not allow a workflow to modify its own workflow files.**
> `.github/workflows/build.yml` must be updated by a **direct commit** — never via a patch zip.

The workflow enforces this: it aborts with a fatal error if a zip contains `.github/`:
```
FATAL: patch zip contains .github/ — aborting to protect workflow files
```

**The correct process when `build.yml` needs to change:**

1. Put all source-code changes in a patch zip as usual (in `zip_update/patches/`)
2. Commit the new `build.yml` directly to `.github/workflows/build.yml` in a separate commit
3. Both commits can be pushed in the same `git push` — just keep them as separate commits

---

## Delivering a Patch

A patch is a ZIP placed in `zip_update/patches/`. The workflow extracts it and processes it automatically on the next push.

### Build flag — control whether APK compilation runs

The **first line** of `zip_update/CHANGELOG.md` acts as a build flag:

| First line | Effect |
|------------|--------|
| `#!build` | Apply patch **and** compile APK, tag a GitHub Release |
| `#!no-build` | Apply patch only — skip compilation, skip release |
| *(absent)* | Same as `#!build` — compile by default |

Use `#!no-build` for patches that only change documentation, restructure files, or make non-functional edits where a new APK is unnecessary.

**Example `CHANGELOG.md` with no-build flag:**
```markdown
#!no-build
# Rename internal helper functions

Renamed StartTimeCompact and DurationChipsCompact to internal visibility.
No behavior changes.
```

**Example `CHANGELOG.md` with build flag:**
```markdown
#!build
# Fix attendance dialog layout

Row 4 split into two independent rows: duration chips on top,
topic field below. Fixes chip text wrapping on narrow screens.
```

The flag line is automatically stripped from commit messages and GitHub Release notes.

### ZIP rules

- Named `update_<unix_timestamp>.zip` (e.g. `update_1773715508.zip`)
- Paths must be **relative to the repo root** — no outer wrapper directory
- Must **not** contain `.github/`

```
✅ Correct                               ❌ Wrong
──────────────────────────────────────   ──────────────────────────────────
app/src/.../ScheduleScreen.kt            some_folder/app/src/...
zip_update/CHANGELOG.md                  .github/workflows/build.yml
zip_update/patches/apply.sh
```

Verify with `unzip -l update_xxx.zip` — paths must start with `app/`, `zip_update/`, etc.

### Creating a zip (run from repo root)

```bash
zip -r zip_update/patches/update_$(date +%s).zip \
  app/src/main/java/com/school/manager/ui/screens/MyScreen.kt \
  zip_update/CHANGELOG.md \
  zip_update/patches/apply.sh   # optional — only if you need a script
```

### What goes in a zip

| File | Required | Purpose |
|------|----------|---------|
| `app/src/.../*.kt` | optional | Source file replacements |
| `zip_update/CHANGELOG.md` | recommended | First line = build flag; rest = release notes and commit message |
| `zip_update/patches/apply.sh` | optional | Script for changes that can't be done by file replacement |

---

## apply.sh — Script-based Patches

For changes beyond simple file replacement (renaming symbols, deleting files, regex substitutions), include `zip_update/patches/apply.sh` in the zip.

**Execution order:**
```
Extract zip → Run apply.sh → [Stamp version] → [Build APK] → Commit → [GitHub Release]
                                      ↑ skipped when #!no-build ↑
```

**Rules:**

| Rule | Detail |
|------|--------|
| Location | `zip_update/patches/apply.sh` inside the zip |
| Working directory | Repo root |
| Shell | `bash` |
| Exit code | Non-zero aborts the build immediately |
| Cleanup | Deleted automatically after execution, along with the zip; `patches/` directory is preserved |
| Optional | If absent, the step is silently skipped |

**Example `apply.sh`:**

```bash
#!/usr/bin/env bash
set -e

# Delete a deprecated file
rm -f app/src/main/java/com/school/manager/ui/screens/OldScreen.kt
echo "Removed OldScreen.kt"

# Rename a symbol across all Kotlin files
find app/src -name '*.kt' -exec sed -i 's/OldName/NewName/g' {} +
echo "Renamed OldName → NewName"
```

---

## zip_update/ Directory Reference

```
zip_update/
├── patches/
│   ├── update_<timestamp>.zip   # patch zip — deleted by workflow after apply
│   └── apply.sh                 # optional script — delivered via zip, deleted after run
├── CHANGELOG.md                 # first line = build flag; rest = release notes
└── repo_snapshot.md             # auto-generated file list after each patch apply
```

| File | Written by | Purpose |
|------|-----------|---------|
| `patches/update_*.zip` | Developer | Contains source files and/or apply.sh |
| `patches/apply.sh` | Developer (via zip) | Post-extract script; deleted after run; `patches/` dir kept |
| `CHANGELOG.md` | Developer (via zip) | Build flag on line 1; release notes below |
| `repo_snapshot.md` | Workflow (auto) | Full file list snapshot after each patch apply |

---

## Project Structure

```
app/src/main/java/com/school/manager/
├── MainActivity.kt
├── Navigation.kt
├── data/
│   └── Models.kt
├── ui/
│   ├── components/
│   │   ├── CommonComponents.kt
│   │   └── FluentComponentAliases.kt
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
└── viewmodel/
    └── AppViewModel.kt
```

---

## AI Rules

This section defines how the AI assistant (Claude) should behave when working on this project.

### Request source files before making changes

When a task involves modifying an existing file, the AI **must ask the user to upload the current version of that file** before generating any output — unless the file's full and exact content is already available in the conversation or project knowledge.

**Why this matters:** The AI's project knowledge snapshot may be outdated. Generating a patch against a stale version of a file can produce diffs that no longer apply cleanly, introduce merge conflicts, or silently break logic that was changed after the snapshot was taken.

**When to ask for uploads:**

| Situation | Action |
|-----------|--------|
| Modifying any `.kt`, `.yml`, `.md`, or other source file | Ask user to upload the current file first |
| The task is additive only (new file with no dependencies on existing code) | Upload not required |
| File content was already uploaded in this conversation | Upload not required |
| File content is confirmed identical to project knowledge snapshot | Upload not required |

**Example prompt the AI should use:**

> Before I generate this patch, could you upload the current `ScheduleScreen.kt`? The project knowledge snapshot may be outdated, and I want to make sure the diff applies cleanly.

### Minimal diffs — change only what is necessary

The AI must limit edits to exactly what is needed for the requested change. It must not:
- Reformat unrelated code
- Rename variables or functions outside the scope of the task
- Reorganize imports unless directly required
- Add or remove comments unrelated to the change

This keeps diffs small, reviewable, and safe to apply.

### Patch delivery format

All source-code changes must be delivered as a `update_<timestamp>.zip` placed at `zip_update/patches/`, following the rules in [Delivering a Patch](#delivering-a-patch).

Changes to `.github/workflows/build.yml` must be delivered as a **direct file download** (never inside a zip), with a reminder to commit it separately.
