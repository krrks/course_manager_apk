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
| **Database** | **Room (SQLite) — 5 tables with FK constraints** |
| Image loading | Coil |
| Serialization | Gson (export / import only) |
| Build | Gradle 8.7 + AGP 8.5 |

---

## Data Storage

All application data is persisted in a **Room SQLite database** (`school_manager.db`), with referential integrity enforced via foreign key constraints:

| FK constraint | On delete |
|--------------|-----------|
| `lessons.classId → classes` | CASCADE |
| `classes.subjectId → subjects` | SET NULL |
| `classes.headTeacherId → teachers` | SET NULL |

**Legacy migration:** On first launch after upgrading from the old JSON-in-SharedPreferences format, the app automatically reads the existing data, imports it into Room, and removes the old SharedPreferences key — no user action required.

**Export / Import:** The JSON and ZIP backup formats are unchanged. Gson is used only at the export/import boundary; the database itself is the single source of truth.

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
3. Regenerates `README.md` from `docs/*.md` source files
4. If not found — proceeds with current source (warning printed in log)
5. On success with APK compilation — commits changes, tags a release, attaches APKs

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

Verify with `unzip -l update_xxx.zip` — paths must start with `app/`, `zip_update/`, `docs/`, etc.

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
| `docs/*.md` | optional | README section updates (regenerated at build time) |
| `zip_update/CHANGELOG.md` | recommended | First line = build flag; rest = release notes and commit message |
| `zip_update/patches/apply.sh` | optional | Script for changes that can't be done by file replacement |

---

## apply.sh — Script-based Patches

For changes beyond simple file replacement (renaming symbols, deleting files, regex substitutions), include `zip_update/patches/apply.sh` in the zip.

**Execution order:**
```
Extract zip → Generate README → Run apply.sh → [Stamp version] → [Build APK] → Commit → [GitHub Release]
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
## AI Rules

This section defines how the AI assistant (Claude) should behave when working on this project.

### Request source files before making changes

When a task involves modifying an existing file, the AI **must ask the user to upload the current version of that file** before generating any output — unless the file's full and exact content is already available in the conversation or project knowledge.

**Why this matters:** The AI's project knowledge snapshot may be outdated.

---

### Keep individual files small (≤ 300 lines)

**Rule:** No single `.kt` source file should exceed **300 lines**. When the AI creates or refactors code that would push a file beyond this limit, it **must split the file** into logically coherent smaller files.

**Why this matters:** Large files consume a disproportionate number of tokens when uploaded for review or editing. Keeping files small means:
- Faster context loading — only the relevant file needs to be uploaded, not a 600-line monolith.
- Cheaper edits — a 150-line file costs roughly half the tokens of a 300-line file.
- Cleaner diffs — patch zips contain only the changed file, not a giant merged blob.

**Naming convention for split files** — use a consistent suffix pattern:

| Original file | Split into |
|---------------|-----------|
| `FooScreen.kt` | `FooScreen.kt` (entry + scaffold) |
| | `FooDialogs.kt` (all dialogs) |
| | `FooComponents.kt` (local-only composables) |
| | `FooHelpers.kt` (pure functions, constants) |

This project already follows this convention for the Lesson screen:
```
LessonScreen.kt        ← entry point & state
LessonViews.kt         ← Week / Month / Day views
LessonListView.kt      ← List view + LessonCard
LessonDialogs.kt       ← detail & form dialogs
LessonBatchDialogs.kt  ← batch generate / modify / delete dialogs
LessonBatchActionDialog.kt ← multi-select batch action dialog
LessonTimeHelpers.kt   ← time math, layout constants, status helpers
LessonFilterSheet.kt   ← filter bottom sheet
```

**When the AI proposes a new feature** that would be added to an existing file, it must first check (or ask) whether that file is already near the 300-line limit. If it is, the AI must propose a split plan before writing any code.

**Exemptions:** Auto-generated or config files (e.g. `Entities.kt`, `Daos.kt`, `Mappers.kt`) are exempt from this limit when their size is structurally determined by the number of database tables.

---

### Three-step workflow for all change requests

When the user raises a requirement or change request, the AI **must follow this three-step process** and **must not skip or merge steps**:

**Step 1 — Solution proposal (no code)**
- Describe what will change and in which files, at a high level.
- Do not write any code or detailed logic.
- End with: "Please confirm the proposal before proceeding to Step 2."
- Wait for the user's explicit confirmation before continuing.

**Step 2 — Change plan (no code)**
- List each file to be modified, the specific location within the file, and exactly what will be added, removed, or replaced.
- Do not write any code.
- End with: "Please confirm the plan before proceeding to Step 3."
- Wait for the user's explicit confirmation before continuing.

**Step 3 — Code generation and ZIP delivery**
- Generate the complete modified file(s) only after Step 2 is confirmed.
- Output one file at a time using the file creation tool.
- **After all files are generated, package them into a ZIP patch file** following the format defined in "Delivering a Patch" (named `update_<timestamp>.zip`, paths relative to repo root, includes `zip_update/CHANGELOG.md`).
- Deliver the ZIP to the user as the primary output — do not rely on pasting raw file content into the chat as the final deliverable.

**Why three steps matter:** Merging planning and coding into one pass causes the AI to spend excessive time in a single reasoning block, which increases the risk of context overflow, repeated rethinking, and output failure. Keeping steps separated ensures each phase is short, focused, and verifiable.

---

### Separate internal reasoning from code output

When executing Step 3 (code generation), the AI **must not interleave extended reasoning with code writing**. The correct approach is:

1. Complete all planning and reasoning *before* starting to write any file.
2. Then output files one by one, without pausing to re-plan between files.
3. If a mid-generation decision is needed (e.g. a naming conflict is discovered), stop, surface the question to the user, and wait — do not silently re-plan and restart.

**Avoid:** Outputting a file, then re-analysing requirements, then outputting a revised version of the same file in the same response. This wastes tokens and confuses the diff.

---

### README.md is auto-generated — do not edit directly

`README.md` is assembled automatically by the CI workflow from source files in `docs/`:

| Source file | Contents |
|-------------|----------|
| `docs/readme_header.md` | Features, Tech Stack, Data Storage |
| `docs/readme_build.md` | Building Locally, CI/CD, Patch delivery, apply.sh |
| `docs/readme_structure.md` | Project Structure |
| `docs/readme_ai_rules.md` | AI Rules (this file) |

**To update the README**, edit the relevant `docs/*.md` file and include it in the patch ZIP. The workflow will regenerate `README.md` automatically on the next build.

**Never include `README.md` directly in a patch ZIP** — it will be overwritten by the workflow anyway.
