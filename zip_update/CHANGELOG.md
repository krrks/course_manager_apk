#!build
# Hotfix: 修复 Release 包闪退（R8 TypeToken 泛型擦除）

## 问题

Release 构建开启 R8 混淆后，`Mappers.kt` 中静态初始化块：

```kotlin
private val longListType = object : TypeToken<List<Long>>() {}.type
```

R8 擦除泛型签名 → Gson 抛出 `IllegalStateException: TypeToken must be
created with a type argument` → `ExceptionInInitializerError` → 闪退。

## 修复

将 `List<Long>` 的 JSON 序列化/反序列化从 Gson `TypeToken` 改为 Android 内置
`org.json.JSONArray`，完全不受 R8/ProGuard 影响，无需添加任何混淆规则。

## 修改文件

- `app/src/main/java/com/school/manager/data/db/Mappers.kt`
