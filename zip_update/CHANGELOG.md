# Restructure zip_update/ and rewrite README

## Changes

### README.md
- Rewritten in English
- Documents new `zip_update/` directory structure and file names
- Added rule: `build.yml` must be updated manually via direct commit, never via zip
- Documents `apply.sh` (replaces `patch.sh`)

### zip_update/ restructure (via apply.sh)
- `update_*.zip` moved to `zip_update/patches/update_*.zip`
- `patch.sh` renamed to `apply.sh`
- `release_note.md` renamed to `CHANGELOG.md`
- `note.md` merged into `CHANGELOG.md` and deleted
- `file_list.md` renamed to `repo_snapshot.md`
- `file_list_befor_update.md` deleted (redundant, typo in name)

### build.yml
- Updated separately via direct commit (not included in this zip)
- Patch zip lookup: `zip_update/update_*.zip` → `zip_update/patches/update_*.zip`
- `patch.sh` reference → `apply.sh`
- Step names cleaned up, internal variable names updated
- `file_list.md` → `repo_snapshot.md`, `release_note.md` → `CHANGELOG.md`
