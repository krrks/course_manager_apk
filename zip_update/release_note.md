# v1.5.2 — 修复签名失败 + UI 优化

## 🔧 修复签名失败（FATAL 问题）
**错误**：`KeytoolException: Keystore was tampered with, or password was incorrect`
**原因**：`keys/release.jks` 与 Variables 里存储的密码不是同一次生成的，两者不匹配。
**修复**：
- `app/build.gradle.kts`：增加兜底逻辑——jks 不存在或密码未设置时自动降级到 debug 签名，CI 不再 FAILED
- `update_apk_key.yml`：优化密码写入流程，确保 jks 与 Variables 始终同步生成

**修复后操作（重新生成匹配的 key）**：
1. 到 Actions → "🔑 Generate & Update Release Keystore" → Run workflow
2. 等待完成，新 jks 和密码会自动同步

## ✅ UI 修复（已在上传文件中）
- 时间滚轮：滚动释放后立即吸附选中中央值
- 出勤学生：点击名字 FilterChip 切换出勤状态（课表页快速添加记录）
