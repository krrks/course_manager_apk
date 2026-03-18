#!no-build
# 更新文档：Room 存储层描述 + 项目结构

- README.md Tech Stack 表格新增 Room 条目，移除 SharedPreferences 相关描述
- 新增 "Data Storage" 章节，说明 FK 约束和 SET NULL / CASCADE 行为
- 更新 Project Structure，加入 data/db/ 和 data/repository/ 子目录
- Serialization 一行改为"仅用于 export / import 边界"，消除歧义
