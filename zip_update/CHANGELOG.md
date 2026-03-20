#!build
# 修复编译错误：LessonDialogs.kt 补全 dp 单位 import

## 变更内容
- `LessonDialogs.kt` 补加 `import androidx.compose.ui.unit.dp`
- 修复上一次重构 patch 遗漏该 import 导致的 36 处编译错误
