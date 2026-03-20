#!no-build
# build.yml: README assembly now driven by docs/readme_order.txt

## Changes

### New file
- `docs/readme_order.txt` — controls assembly order of README sections;
  add new sections here without touching build.yml

### Modified file
- `.github/workflows/build.yml` — generate README step rewritten:
  reads docs/readme_order.txt line by line; falls back to alphabetical
  sort if order file absent; skips missing files with a warning

### How to add a new README section in future
1. Create `docs/readme_mynewsection.md`
2. Add `readme_mynewsection.md` at the desired line in `docs/readme_order.txt`
3. Include both files in the patch zip — no build.yml change needed
