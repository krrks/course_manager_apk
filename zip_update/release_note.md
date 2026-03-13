# 签名方案升级：密码随仓库存储，不再依赖 Variables

## 变更内容

### update_apk_key.yml
- 生成 keystore 后，把密码写入 `keys/keystore.properties`（与 `release.jks` 一起 commit）
- 移除原来写 Repository Variables 的步骤（不再需要 PAT_TOKEN）

### build.yml
- 编译前新增 Step 8「Load keystore properties」，从 `keys/keystore.properties` 读取密码
- 密码通过 `$GITHUB_ENV` 传给后续 step，Gradle 通过 `System.getenv()` 读取
- 移除所有 `vars.KEYSTORE_*` 引用

## 使用流程
1. push 这两个 workflow 文件
2. 到 Actions → 🔑 Generate & Update Release Keystore → Run workflow
3. 确认 keys/release.jks 和 keys/keystore.properties 已自动 commit
4. 之后每次 push 自动编译并使用正确签名
