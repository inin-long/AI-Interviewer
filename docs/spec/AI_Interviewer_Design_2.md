# 第二部分：Maven 工程结构、软件分层设计、数据库设计、文件系统设计

------

# 8. Maven 工程结构

## 8.1 工程类型

采用：

> 单 Maven Module 工程

原因：

本项目是单机桌面应用，不拆分多个独立服务。

不采用：

- Maven Multi Module
- 独立 Agent 服务
- 独立后端服务

------

## 8.2 项目目录

最终结构：

```
ai-interviewer/

├── pom.xml

├── src/

│
├── main/

│   ├── java/

│   │
│   │   └── com.inin.aiinterviewer/

│   │
│   └── resources/


└── test/

    └── java/
```

------

# 9. Java Package 分层设计

完整 Package：

```
com.inin.aiinterviewer

├── ApplicationLauncher.java


├── ui/

│   ├── controller/

│   ├── component/

│   ├── view/

│   ├── navigation/

│   └── state/


├── application/

│   ├── service/

│   ├── dto/

│   ├── mapper/

│   ├── event/

│   └── exception/


├── domain/

│   ├── entity/

│   ├── model/

│   └── enums/


├── infrastructure/

│   ├── database/

│   ├── file/

│   ├── ai/

│   ├── vector/

│   └── task/


├── agent/

│   ├── graph/

│   ├── node/

│   ├── state/

│   ├── tool/

│   ├── prompt/

│   └── stage/


└── config/

    ├── properties/

    └── security/
```

------

# 10. 分层职责说明

------

## 10.1 UI Layer

路径：

```
ui/
```

职责：

负责：

- JavaFX 页面
- 用户交互
- UI 状态展示
- 页面导航

包含：

- Controller
- FXML
- Component
- ViewModel

------

禁止：

UI 直接访问：

```
❌ Mapper

❌ SQLite

❌ AI Client

❌ File System
```

------

调用关系：

正确：

```
Controller

↓

Application Service

↓

Domain / Infrastructure
```

------

## 10.2 Application Layer

路径：

```
application/
```

职责：

业务流程编排。

例如：

创建面试：

```
InterviewService

↓

创建 Session

↓

加载 Profile

↓

初始化 Agent

↓

启动面试
```

------

包含：

## Service

例如：

```
UserService

ResumeService

InterviewService

ReportService

DocumentService
```

------

## DTO

用于跨层传输：

例如：

```
ResumeDTO

InterviewDTO

ReportDTO
```

------

## Mapper

负责：

Entity → DTO

例如：

```
ResumeMapper
```

------

## Event

Spring ApplicationEvent：

例如：

```
ResumeCreatedEvent

InterviewCompletedEvent
```

------

## 10.3 Domain Layer

路径：

```
domain/
```

职责：

核心业务模型。

不依赖：

- Spring
- JavaFX
- SQLite

------

包含：

## Entity

数据库实体：

例如：

```
UserEntity

ResumeEntity

InterviewEntity
```

------

## Model

业务对象：

例如：

```
CandidateProfile

InterviewContext

EvaluationResult
```

------

## Enum

例如：

```
InterviewStage

TaskStatus

ReportStatus
```

------

## 10.4 Infrastructure Layer

路径：

```
infrastructure/
```

负责：

外部系统交互。

------

包含：

## database

负责：

- SQLite
- MyBatis Mapper

------

## file

负责：

- 文件读写
- 路径管理

核心：

```
PathService

FileStorageService
```

------

## ai

负责：

- Spring AI
- LLM调用
- RetryService

------

## vector

负责：

- Lucene Vector Store
- Embedding索引

------

## task

负责：

- 后台任务
- Worker

------

## 10.5 Agent Layer

路径：

```
agent/
```

负责：

AI 面试逻辑。

包含：

- Graph
- Node
- State
- Tool
- Prompt

------

# 11. 数据库设计

## 11.1 数据库类型

使用：

SQLite

文件：

```
AI-Interviewer/database/app.db
```

------

## 11.2 数据库设计原则

## 主键

统一：

```
BIGINT
```

------

## 时间字段

所有业务表：

包含：

```
create_time

update_time
```

------

## 删除策略

采用：

逻辑删除。

字段：

```
deleted INTEGER DEFAULT 0
```

------

## 枚举存储

使用：

字符串。

例如：

正确：

```
status VARCHAR(32)
```

不使用：

```
1
2
3
```

------

# 12. 核心数据表设计

------

## 12.1 user 用户表

用途：

保存本地用户。

```
CREATE TABLE user (

    id INTEGER PRIMARY KEY,

    username VARCHAR(64) NOT NULL UNIQUE,

    password_hash VARCHAR(255) NOT NULL,

    nickname VARCHAR(64),

    create_time DATETIME,

    update_time DATETIME,

    deleted INTEGER DEFAULT 0

);
```

------

字段：

| 字段          | 说明       |
| ------------- | ---------- |
| id            | 用户ID     |
| username      | 用户名     |
| password_hash | BCrypt密码 |
| nickname      | 昵称       |
| create_time   | 创建时间   |

------

## 12.2 resume 简历表

用途：

保存用户简历。

```
CREATE TABLE resume (

    id INTEGER PRIMARY KEY,

    user_id INTEGER NOT NULL,

    original_name VARCHAR(255),

    storage_path VARCHAR(512),

    status VARCHAR(32),

    create_time DATETIME,

    update_time DATETIME

);
```

------

状态：

```
UPLOADED

PARSING

COMPLETED

FAILED
```

------

## 12.3 candidate_profile 候选人画像表

用途：

保存 AI 从简历提取的信息。

```
CREATE TABLE candidate_profile (

    id INTEGER PRIMARY KEY,

    resume_id INTEGER,

    user_id INTEGER,

    content_json TEXT,

    create_time DATETIME,

    update_time DATETIME

);
```

------

content_json：

保存：

```
{
 "skills":[],
 "projects":[],
 "experience":[]
}
```

------

## 12.4 interview_session 面试会话表

用途：

保存一次完整面试。

```
CREATE TABLE interview_session (

    id INTEGER PRIMARY KEY,

    user_id INTEGER,

    resume_id INTEGER,

    title VARCHAR(128),

    stage VARCHAR(64),

    status VARCHAR(32),

    create_time DATETIME,

    update_time DATETIME

);
```

------

状态：

```
CREATED

RUNNING

PAUSED

COMPLETED

FAILED
```

------

## 12.5 message 面试消息表

用途：

保存聊天记录。

```
CREATE TABLE message (

    id INTEGER PRIMARY KEY,

    session_id INTEGER,

    role VARCHAR(32),

    content TEXT,

    create_time DATETIME

);
```

------

role：

```
USER

ASSISTANT

SYSTEM
```

------

## 12.6 report 报告表

```
CREATE TABLE report (

    id INTEGER PRIMARY KEY,

    interview_id INTEGER,

    title VARCHAR(255),

    content_markdown TEXT,

    score INTEGER,

    status VARCHAR(32),

    create_time DATETIME,

    update_time DATETIME

);
```

------

状态：

```
GENERATING

COMPLETED

FAILED
```

------

## 12.7 task 任务表

用于异步任务。

```
CREATE TABLE task (

    id INTEGER PRIMARY KEY,

    user_id INTEGER,

    task_type VARCHAR(64),

    status VARCHAR(32),

    progress INTEGER,

    payload_json TEXT,

    error_message TEXT,

    create_time DATETIME,

    update_time DATETIME

);
```

------

任务类型：

```
RESUME_PARSE

DOCUMENT_PARSE

EMBEDDING_GENERATE

VECTOR_UPDATE
```

------

## 12.8 agent_checkpoint 表

用于 LangGraph 状态恢复。

```
CREATE TABLE agent_checkpoint (

    id INTEGER PRIMARY KEY,

    session_id INTEGER,

    node_name VARCHAR(128),

    state_json TEXT,

    version VARCHAR(32),

    create_time DATETIME,

    update_time DATETIME

);
```

------

## 12.9 document 表

知识库文档。

```
CREATE TABLE document (

    id INTEGER PRIMARY KEY,

    user_id INTEGER,

    name VARCHAR(255),

    storage_path VARCHAR(512),

    status VARCHAR(32),

    create_time DATETIME,

    update_time DATETIME

);
```

------

## 12.10 document_chunk 表

文档切片。

```
CREATE TABLE document_chunk (

    id INTEGER PRIMARY KEY,

    document_id INTEGER,

    chunk_index INTEGER,

    content TEXT,

    token_count INTEGER,

    vector_id VARCHAR(128),

    create_time DATETIME

);
```

------

# 13. 数据关系

整体关系：

```
User

 ├── Resume

 │       └── CandidateProfile

 │

 └── InterviewSession

          ├── Message

          ├── AgentCheckpoint

          └── Report


User

 └── Document

          └── DocumentChunk
```

------

# 14. 文件系统设计

根目录：

```
AI-Interviewer/
```

------

完整结构：

```
AI-Interviewer/

├── database/

│   └── app.db


├── users/

│
│   └── {user-id}/

│       ├── resumes/

│       ├── documents/

│       ├── reports/

│       └── vector/


├── logs/


├── temp/


└── config/

    └── application-local.yml
```

------

# 15. 文件访问规则

所有文件路径：

必须经过：

```
PathService
```

------

禁止：

```
"user/" + id + "/file"
```

这种硬编码。

------

正确：

```
pathService.getResumePath(userId);
```

------

# 16. 用户文件命名规则

禁止：

```
resume.pdf
```

避免冲突。

------

采用：

```
UUID_originalName
```

例如：

```
a81c2f_resume.pdf
```

数据库保存：

```
original_name

storage_name
```
