# Release Notes — v1.5.1-fix / 更新说明

## 修复编译错误
`sampleAppState()` 函数移到文件末尾（所有 sample 数据定义之后），
解决 `Unresolved reference 'sampleSchedule'` 编译错误。

AppViewModel.kt 保持不变（同上一个 patch）。
