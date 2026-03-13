# v1.5.2 — 彻底修复签名崩溃

## 问题根因
上一个 patch 的 build.gradle.kts 在 `signingConfigs.create("release")` 块内部
调用了 `signingConfigs.getByName("debug")`，触发 Gradle 配置阶段循环初始化异常，
导致签名配置本身就失败，进而 packageRelease FAILED。

## 本次修复
将"是否使用 release key"的判断提到 `android {}` 顶层：
- 四个环境变量全部存在 AND jks 文件真实存在 → 创建 release signingConfig 并使用
- 任何一个缺失 → `signingConfig = signingConfigs.getByName("debug")`（debug key，不会崩溃）
- `signingConfigs.create("release")` 块内再也不引用 debug config，避免循环依赖

## 后续：让 release APK 使用正式签名
1. 手动提交 `.github/workflows/update_apk_key.yml`（上次已提供）
2. 到 Actions → 🔑 Generate & Update Release Keystore → Run workflow
3. 之后编译自动使用匹配的 jks 签名
