# 第六部分：配置系统、异常处理、日志系统、测试规范、开发约束

本部分定义项目运行配置、错误处理、日志记录、测试策略以及 AI 编码 Agent 必须遵守的开发规则。

目标：

确保：

- 配置统一管理
- 异常可追踪
- 日志可定位
- 代码结构稳定
- 低智能模型不会破坏架构

------

# 58. 配置系统设计

## 58.1 配置原则

采用：

> Spring Boot YAML 配置体系

配置优先级：

```
环境变量

        ↓

application-local.yml

        ↓

application.yml
```

------

最高优先级：

环境变量。

------

# 58.2 配置文件结构

## 默认配置

位置：

```
src/main/resources/

└── application.yml
```

------

示例：

```
id="q7m3x5"
app:

  name: AI Interviewer

  version: 1.0.0



database:

  path: ./database/app.db



storage:

  root-path: ./users



llm:

  base-url:

  api-key:

  chat-model:

  embedding-model:



task:

  worker-count: 2

  retry-count: 3
```

------

## 用户配置

位置：

```
AI-Interviewer/

└── config/

    └── application-local.yml
```

------

示例：

```
id="x4m8q2"
llm:

  base-url: https://api.example.com

  chat-model: xxx-chat

  embedding-model: xxx-embedding
```

------

# 59. 配置模块设计

目录：

```
config/

├── properties/

│
├── AppProperties.java

├── LLMProperties.java

├── StorageProperties.java

├── TaskProperties.java


└── ConfigValidator.java
```

------

# 59.1 Properties 类

使用：

```
@ConfigurationProperties
```

------

示例：

```
@Component
@ConfigurationProperties(prefix="llm")
public class LLMProperties {


    private String baseUrl;


    private String apiKey;


    private String chatModel;


}
```

------

禁止：

大量使用：

```
@Value("${xxx}")
```

------

原因：

配置分散，不利维护。

------

# 59.2 配置校验

启动流程：

```
Application Start

↓

Load Config

↓

ConfigValidator

↓

Pass

↓

Start UI
```

------

检查：

## AI配置

例如：

```
base-url为空

↓

提示用户配置
```

------

## 文件目录

检查：

```
storage路径不存在

↓

创建目录
```

------

## 数据库

检查：

```
SQLite文件是否可访问
```

------

# 60. 敏感信息管理

## API Key

禁止：

提交：

```
application.yml
```

------

允许：

环境变量：

```
AI_LLM_API_KEY=xxxx
```

------

或者：

本机：

```
application-local.yml
```

------

禁止：

```
❌ 明文写入代码

❌ Git提交Key

❌ 日志输出Key
```

------

# 61. 异常处理体系

## 61.1 设计目标

统一：

- 异常分类
- 日志记录
- UI展示

------

流程：

```
Exception

↓

GlobalExceptionHandler

↓

Logger

↓

UI ErrorView
```

------

# 62. 异常层级

目录：

```
application/

└── exception/

    ├── BusinessException.java

    ├── AIException.java

    ├── FileException.java

    ├── DataException.java

    ├── TaskException.java

    └── SystemException.java
```

------

# 62.1 BusinessException

业务错误。

例如：

```
用户名已存在

简历不存在

状态非法
```

------

# 62.2 AIException

AI错误。

例如：

```
模型调用失败

API超时

返回格式错误
```

------

# 62.3 FileException

文件错误。

例如：

```
文件不存在

解析失败

权限不足
```

------

# 62.4 DataException

数据库错误。

例如：

```
SQLite异常

Migration失败
```

------

# 62.5 TaskException

任务错误。

例如：

```
Embedding失败

解析任务失败
```

------

# 62.6 SystemException

系统级错误。

例如：

```
配置错误

线程异常

未知异常
```

------

# 63. ErrorCode 设计

统一错误码：

```
AI_001

FILE_001

USER_001

DATA_001
```

------

目录：

```
exception/

└── ErrorCode.java
```

------

示例：

```
public enum ErrorCode {


    AI_TIMEOUT,

    FILE_NOT_FOUND,

    USER_EXISTED

}
```

------

# 64. UI 异常展示

禁止：

直接显示：

```
NullPointerException
```

------

必须转换：

例如：

后台：

```
AIException:
Connection timeout
```

------

UI：

```
AI服务连接失败

请检查网络配置。
```

------

使用：

```
ErrorView
```

------

# 65. 日志系统

## 65.1 技术

采用：

```
SLF4J

+

Logback
```

------

目录：

```
resources/

└── logback-spring.xml
```

------

# 65.2 日志目录

```
AI-Interviewer/

└── logs/

    ├── app.log

    └── error.log
```

------

# 66. 日志级别

## DEBUG

开发调试：

例如：

```
Agent Node执行

Tool调用参数
```

------

## INFO

正常流程：

例如：

```
用户登录

任务完成

面试开始
```

------

## WARN

异常但可恢复：

例如：

```
AI重试

文件跳过
```

------

## ERROR

严重错误：

例如：

```
数据库失败

任务失败
```

------

# 67. 日志规范

禁止：

输出：

```
log.info(password);
```

------

禁止：

```
log.info(apiKey);
```

------

允许：

```
log.info(
"Interview started: {}",
sessionId
);
```

------

# 68. 测试规范

## 68.1 测试技术

采用：

- JUnit5
- Mockito
- Spring Boot Test

------

# 69. 测试范围

------

# 69.1 Service 单元测试

重点测试：

例如：

```
ResumeService

InterviewService

ReportService
```

------

测试：

- 输入
- 输出
- 异常

------

# 69.2 Agent 测试

测试：

Node：

例如：

```
QuestionGeneratorNode

EvaluationNode
```

------

方式：

Mock：

```
ChatService
```

------

不直接调用真实 LLM。

------

# 69.3 Tool 测试

例如：

```
KnowledgeSearchTool
```

测试：

```
Input

↓

Mock VectorStore

↓

Result
```

------

# 69.4 Repository 测试

测试：

SQLite：

- CRUD
- Migration

------

# 70. 测试目录

结构：

```
src/test/java/

├── service/

├── agent/

├── repository/

└── infrastructure/
```

------

# 71. 编码规范

## 71.1 命名规范

类：

```
PascalCase
```

例如：

```
InterviewService
```

------

方法：

```
camelCase
```

例如：

```
createInterview()
```

------

常量：

```
UPPER_CASE
```

例如：

```
MAX_RETRY_COUNT
```

------

# 72. 分层约束

必须遵守：

```
UI

↓

Application

↓

Domain

↓

Infrastructure
```

------

禁止：

## Controller访问Mapper

错误：

```
controller

↓

mapper
```

------

## Agent访问数据库

错误：

```
Agent

↓

SQLite
```

------

## Entity暴露UI

错误：

```
Entity

↓

JavaFX
```

------

# 73. AI 编码 Agent 开发规则

由于本项目会交给低智能模型辅助编码，必须严格遵守以下规则。

------

# 73.1 开发前检查

编码前：

必须阅读：

- 本设计文档
- 当前目录结构
- 已存在代码

------

禁止：

直接创建重复模块。

------

# 73.2 不允许架构改变

除非用户明确要求：

禁止：

```
❌ 引入微服务

❌ 修改技术栈

❌ 改数据库

❌ 替换Agent框架

❌ 修改分层结构
```

------

# 73.3 优先复用

新增功能前：

检查：

是否已有：

- Service
- Component
- Tool
- Event

------

禁止：

重复实现。

------

# 73.4 代码质量要求

必须：

- 添加必要注释
- 保持方法职责单一
- 避免超大类
- 避免静态全局变量

------

# 74. 打包发布规范

## 74.1 打包目标

桌面应用。

------

采用：

Java Package Tool：

```
jpackage
```

------

输出：

Windows：

```
.exe
```

------

# 74.2 运行目录

安装后：

```
AI-Interviewer/

├── app/

├── runtime/

└── config/
```

------

用户数据：

独立：

```
AppData/AI-Interviewer/
```

------

# 74.3 升级策略

V1：

不实现自动更新。
