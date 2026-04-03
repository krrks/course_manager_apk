## Deprecated Symbols

This section tracks deprecated functions and symbols that have appeared in build warnings.
AI must avoid using these and prefer the listed replacements.

| Deprecated | Replacement | File / Context | Source |
|------------|-------------|----------------|--------|
| `Icons.Outlined.MenuBook` | `Icons.AutoMirrored.Outlined.MenuBook` | `Navigation.kt` — Subjects screen icon | Build warning v1.029 |
| `ExposedDropdownMenuBox` + `.menuAnchor()` (no-arg) | `.menuAnchor(MenuAnchorType.PrimaryNotEditable)` | `CommonComponents.kt`, `FluentComponentAliases.kt` — dropdowns | Kapt warning |
| `MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)` + 5-arg `EncryptedSharedPreferences.create(fileName, keyAlias, context, …)` | `MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()` + 5-arg `EncryptedSharedPreferences.create(context, fileName, masterKey, …)` | `GitHubSyncViewModel.kt` — `openPrefs()` | Build warning v1.030 |

---
