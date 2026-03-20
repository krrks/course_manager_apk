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
