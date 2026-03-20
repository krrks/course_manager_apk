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
