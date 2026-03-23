#!no-build
# 新增 deprecated 符号记录文档

新增 docs/readme_deprecated.md，记录 AI 使用过的 deprecated 函数/符号及推荐替代写法，
避免后续重复使用。更新 docs/readme_order.txt 将其合并进 README。

当前记录：
- Icons.Outlined.MenuBook → Icons.AutoMirrored.Outlined.MenuBook
- menuAnchor() (no-arg) → menuAnchor(MenuAnchorType.PrimaryNotEditable)
