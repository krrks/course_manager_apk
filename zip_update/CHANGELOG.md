#!build
# 修复废弃 API 警告：MasterKeys → MasterKey.Builder

修复 GitHubSyncViewModel.kt 中使用已废弃的 security-crypto API 导致的编译警告：

- 将 `MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)` 替换为
  `MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()`
- 将废弃的 5 参数 `EncryptedSharedPreferences.create(fileName, keyAlias, context, …)` 替换为
  新签名 `EncryptedSharedPreferences.create(context, fileName, masterKey, …)`
- 更新 docs/readme_deprecated.md，记录此废弃符号及替代方案
