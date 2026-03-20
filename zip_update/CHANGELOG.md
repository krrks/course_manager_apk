#!no-build
# README split into docs/ source files; build.yml auto-assembles README on each build

## Changes

### New files
- `docs/readme_header.md` — Features, Tech Stack, Data Storage
- `docs/readme_build.md` — Building, CI/CD, Patch delivery, apply.sh, zip_update reference
- `docs/readme_structure.md` — Project Structure
- `docs/readme_ai_rules.md` — AI Rules (full English, ZIP delivery rule added)

### Modified files
- `README.md` — replaced with auto-generation stub comment
- `.github/workflows/build.yml` — added step 4 "Generate README.md from docs/" between apply.sh and commit

### Notes
- docs/ changes trigger build (relevance check updated to include docs/)
- README.md is never edited directly; always update docs/*.md instead
- build.yml must be committed directly, not via patch zip
