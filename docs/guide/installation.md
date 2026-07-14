# 安装部署

## 环境要求

### 必需

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 21.x | 推荐 Oracle JDK 或 Adoptium |
| 操作系统 | Windows 10/11 x64 | 当前仅支持 Windows |

### 可选

| 依赖 | 版本 | 说明 |
|------|------|------|
| Maven | 3.9.x | 首次生成 Wrapper 时需要 |
| IDE | IntelliJ IDEA | 推荐开发环境 |

---

## 获取代码

```bash
git clone <repository-url>
cd AI-Interviewer
```

---

## 配置 LLM 服务

AI Interviewer 需要连接 OpenAI 兼容的 LLM 服务。支持的服务商包括：

- DeepSeek（推荐）
- OpenAI
- 其他 OpenAI 兼容 API

### 方式一：配置文件

创建 `src/main/resources/application-local.yml`：

```yaml
llm:
  base-url: https://api.deepseek.com
  api-key: sk-your-api-key-here
  chat-model: deepseek-chat
  embedding-model: bge-m3
  timeout: 60
```

### 方式二：环境变量

```bash
set LLM_BASE_URL=https://api.deepseek.com
set LLM_API_KEY=sk-your-api-key-here
set LLM_CHAT_MODEL=deepseek-chat
set LLM_EMBEDDING_MODEL=bge-m3
```

### 验证配置

启动应用后，在「设置」页面点击「测试连接」验证 API 配置是否正确。

---

## 构建和运行

### 使用 Maven Wrapper（推荐）

```bash
# 运行全部测试
.\mvnw.cmd clean test

# 启动应用
.\mvnw.cmd javafx:run
```

### 使用系统 Maven

```bash
# 首次生成 Wrapper（需要 Maven 3.9+）
mvn wrapper:wrapper

# 之后使用 Wrapper
.\mvnw.cmd clean test
.\mvnw.cmd javafx:run
```

---

## 数据目录

应用启动后会在用户目录下创建数据目录：

```
AI-Interviewer/
├── database/app.db          # SQLite 数据库
├── users/{user-id}/         # 用户数据
├── logs/                    # 应用日志
├── temp/                    # 临时文件
└── config/                  # 配置文件
```

---

## 常见问题

### JDK 版本不对

```
错误：UnsupportedClassVersionError
解决：确保使用 JDK 21，执行 java -version 检查
```

### API Key 未配置

```
错误：AI 服务未配置
解决：检查 application-local.yml 或环境变量是否正确设置
```

### 数据库迁移失败

```
错误：FlywayException
解决：检查数据库文件权限，或删除 database/app.db 重新启动
```

---

## 下一步

- [日常使用](./usage.md) - 完整的使用流程
- [Windows 发布](../product/roadmap.md) - 打包和分发
