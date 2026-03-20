#!build
# 重构导入导出：统一 ZIP 格式，增加导入预览

## 变更内容

移除独立 JSON 导出/导入，统一为单一 ZIP 格式。

### 新增文件
- `data/BackupModels.kt` — BackupMeta / BackupCounts / FilterDescription / ImportResult / filterForExport()
- `viewmodel/BackupManager.kt` — ZIP 构建与解析，与 ViewModel 解耦
- `ui/screens/ExportImportDialog.kt` — 导入前预览弹窗

### 修改文件
- `viewmodel/AppViewModel.kt` — 删除旧 exportFullStateJson / exportLessonsJson / exportFullBackupZip / importFullBackupZip / importMerge，新增 exportFullZip / exportFilteredZip / peekImportZip / commitImportZip
- `ui/screens/ExportScreen.kt` — 全部 JSON 入口替换为两个 ZIP 入口（完整 + 筛选），导入走预览弹窗

### ZIP 格式（schema v2）
```
meta.json   ← BackupMeta（版本、时间、各实体数量、筛选描述）
state.json  ← AppState（与旧版相同结构，向后兼容）
avatars/    ← 头像图片（仅完整导出包含）
```

### 向后兼容
旧版 ZIP（无 meta.json）导入时 peekZip 返回 Failure，用户会看到"旧版备份"提示而非崩溃。
state.json 结构未变，parseGsonState() 无需修改。
