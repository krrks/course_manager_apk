# 修复 keytool 报密码错误

## 问题原因
keys/release.jks 已存在于仓库，keytool -genkeypair 用新随机密码尝试
打开旧 jks，导致 "Keystore was tampered with, or password was incorrect"。

## 修复内容
update_apk_key.yml：生成前先 rm -f keys/release.jks，再新建。
同时修复 keystore.properties 写入方式（用 printf 替换 heredoc 避免缩进空格）。

build.yml 无变化。
