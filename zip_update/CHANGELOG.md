#!build
# 修复知识点页面安装后空白的问题

根本原因：viewModelScope.launch {} 默认使用 Dispatchers.Main，导致
seedKnowledgePoints 在主线程读取 assets 文件时在部分设备上静默失败。

修改内容：
- AppViewModel.init：改为 viewModelScope.launch(Dispatchers.IO)，确保初始化
  协程从一开始就在 IO 线程运行，避免主线程阻塞或静默异常
- AppRepository.seedKnowledgePoints：整体用 withContext(Dispatchers.IO) 包裹，
  文件读取改用 bufferedReader(Charsets.UTF_8) 显式指定编码，确保 JSON 解析
  不受设备默认字符集影响
- 以上两处改动协同保证 knowledge_points.json 中的所有初中物理知识点在首次
  安装后正确写入数据库并显示在知识点管理页面
