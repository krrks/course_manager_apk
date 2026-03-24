# Knowledge Points — Schema & Convention

## JSON Asset Format

File: `app/src/main/assets/knowledge_points.json`

This file is the **seed source** for built-in knowledge points. It is bundled inside the APK and read once on first launch to populate the Room `knowledge_points` table. After seeding, Room is the single source of truth.

### Record Schema

```json
{
  "id":       1001,
  "grade":    "初中",
  "chapter":  "第1章 机械运动",
  "section":  "第1节 长度和时间的测量",
  "code":     "1.1.1",
  "content":  "知识点描述文字",
  "isCustom": false
}
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Unique ID. Built-in points use 1001–9999 range. User-added points use `System.currentTimeMillis()`. |
| `grade` | String | `"初中"` or `"高中"` |
| `chapter` | String | Chapter name, e.g. `"第1章 机械运动"` |
| `section` | String | Section name, e.g. `"第1节 长度和时间的测量"` |
| `code` | String | Dot-separated index, e.g. `"1.1.1"` = chapter 1, section 1, point 1 |
| `content` | String | Full text of the knowledge point |
| `isCustom` | Boolean | `false` for seeded points; `true` for user-added points |

## Adding More Points

To add built-in knowledge points for additional chapters or grades:

1. Edit `app/src/main/assets/knowledge_points.json`
2. Assign IDs in a consistent range (e.g. 高中 points start at 5001)
3. Include the updated JSON in the next patch ZIP
4. The seed only runs when the table is **empty** — existing installs will NOT receive new built-in points automatically unless a migration or re-seed mechanism is added

## ID Range Convention

| Range | Purpose |
|-------|---------|
| 1001 – 1999 | 初中·第1章 |
| 2001 – 2999 | 初中·第2章 |
| ... | ... |
| 5001 – 8999 | 高中 chapters |
| `System.currentTimeMillis()` | User-added custom points |

## MD → JSON Convention

The source Markdown file (`物理知识点_初中.md`) uses the pattern:

```
- **X.Y.Z** 知识点描述？
```

Each bullet becomes one JSON record:
- `code` = `"X.Y.Z"`
- `chapter` = the `## 第X章` heading above it
- `section` = the `### 第Y节` heading above it
- `content` = the description text (question rephrased as statement where appropriate)
