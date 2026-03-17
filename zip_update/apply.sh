#!/usr/bin/env bash
set -e

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  MIGRATION: zip_update/ restructure"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 1. Create patches/ subdirectory
mkdir -p zip_update/patches
echo "✅ Created zip_update/patches/"

# 2. Rename file_list.md → repo_snapshot.md (keep content, update header)
if [ -f "zip_update/file_list.md" ]; then
  # Rewrite header to English style expected by new workflow
  python3 - <<'PYEOF'
import pathlib, re
p = pathlib.Path("zip_update/file_list.md")
src = p.read_text(encoding="utf-8")
# Replace Chinese header lines with English equivalents
src = re.sub(r'# 🗂 仓库完整文件列表（patch 后快照）', '# Repository Snapshot', src)
src = re.sub(r'> 生成时间 \(UTC\):', '> Updated:', src)
src = re.sub(r'> 提交 SHA:', '> Commit: ', src)
pathlib.Path("zip_update/repo_snapshot.md").write_text(src, encoding="utf-8")
print("✅ Converted file_list.md → repo_snapshot.md")
PYEOF
  rm -f zip_update/file_list.md
  echo "✅ Removed old file_list.md"
fi

# 3. Remove file_list_befor_update.md (typo name, redundant)
if [ -f "zip_update/file_list_befor_update.md" ]; then
  rm -f zip_update/file_list_befor_update.md
  echo "✅ Removed file_list_befor_update.md"
fi

# 4. Rename note.md → CHANGELOG.md (merge with release_note.md if both exist)
if [ -f "zip_update/note.md" ] && [ -f "zip_update/release_note.md" ]; then
  # release_note.md from the zip is the current entry; note.md is old history
  # Keep release_note.md content as new CHANGELOG.md
  cp zip_update/release_note.md zip_update/CHANGELOG.md
  rm -f zip_update/note.md
  rm -f zip_update/release_note.md
  echo "✅ Merged note.md + release_note.md → CHANGELOG.md"
elif [ -f "zip_update/release_note.md" ]; then
  mv zip_update/release_note.md zip_update/CHANGELOG.md
  echo "✅ Renamed release_note.md → CHANGELOG.md"
elif [ -f "zip_update/note.md" ]; then
  mv zip_update/note.md zip_update/CHANGELOG.md
  echo "✅ Renamed note.md → CHANGELOG.md"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  New zip_update/ layout:"
echo "    zip_update/patches/         ← drop update_*.zip here"
echo "    zip_update/apply.sh         ← optional script (this file)"
echo "    zip_update/CHANGELOG.md     ← release notes for current patch"
echo "    zip_update/repo_snapshot.md ← auto-updated by workflow"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
