#!build
# 修复编译错误：Migration import + SubjectsScreen DropdownField 参数名

## 修复

**AppDatabase.kt**
- 补全缺失的 import：`androidx.room.migration.Migration` 和 `androidx.sqlite.db.SupportSQLiteDatabase`
- 显式声明 `val MIGRATION_1_2: Migration = object : Migration(1, 2) { ... }` 以解决类型推断问题

**SubjectsScreen.kt**
- `DropdownField` 改为位置参数调用（与 ClassesScreen 保持一致），修复 "No parameter with name 'value' found" 编译错误
- 将颜色循环内的局部变量 `selected` 重命名为 `isSelected`，避免与 Compose 组件参数名冲突
