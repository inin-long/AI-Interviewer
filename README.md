# AI Interviewer

AI Interviewer 是一款本地运行的 JavaFX AI 技术面试训练工具。项目采用单进程、单 Maven Module 架构，由 Spring Boot 管理业务与基础设施，SQLite 保存本地数据。

## 环境要求

- Windows 10/11 x64
- JDK 21
- 首次生成 Wrapper 时需要 Maven 3.9；之后使用仓库内的 Maven Wrapper

本机开发时请确保 `JAVA_HOME` 指向 JDK 21，例如：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.2'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

## 运行

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd javafx:run
```

首次启动会在 `%LOCALAPPDATA%\AI-Interviewer` 创建数据库、配置、日志和用户文件目录。设置 `AI_INTERVIEWER_HOME` 可覆盖该位置。

## 当前里程碑

已建立首轮可运行骨架：

- Spring Boot 与 JavaFX 生命周期集成
- FXML Controller 的 Spring 注入
- 登录、注册与主窗口壳
- SQLite、Flyway、MyBatis 与本地用户系统
- 分层包结构、配置、异常、日志和文件存储边界
- Agent、AI、向量存储接口和完整 MVP 数据库迁移
- 简历上传、Tika 文本解析、列表与删除
- 面试方案的新建、编辑、复制、删除及简历关联
- 列表页与查看/编辑页分离，并提供可返回的内容区子页面导航
- 候选人画像生成、人工编辑与确认；未配置 AI 时使用明确标记的本地草稿

后续按照 `docs/spec` 中的阶段顺序实现 Agent 面试流程、RAG、报告和 UI 完善。

## 本地配置

不要把 API Key 写入仓库。可以使用环境变量：

```powershell
$env:AI_LLM_API_KEY='...'
```

或复制 `%LOCALAPPDATA%\AI-Interviewer\config\application-local.example.yml` 为 `application-local.yml` 后填写。
