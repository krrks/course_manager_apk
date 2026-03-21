#!no-build
# build.yml: replace mtime dedup with applied_patches.txt

## Changes

### New file
- `zip_update/applied_patches.txt` — tracks the last 20 applied patch names;
  checked at start of each run to skip already-applied zips

### Modified file
- `.github/workflows/build.yml`:
  - check job: remove "Check if patch already released" step (mtime + gh release grep)
  - check job: remove `patch_mtime` output; add "Check if patch already applied"
    step that greps applied_patches.txt by zip filename
  - check job: "Decide whether to build" uses ALREADY_APPLIED instead of ALREADY_RELEASED
  - build job Step 5: before git add -A, append patch name to applied_patches.txt
    and truncate to newest 20 lines; file is committed in the same pass
  - build job Step 15: remove patch_mtime from Release body

## Why
- mtime is unstable (changes on checkout/rsync/re-upload), causing false negatives
- no-build patches were not protected by the old mtime check (no Release is created)
- applied_patches.txt is auditable: `cat zip_update/applied_patches.txt`
