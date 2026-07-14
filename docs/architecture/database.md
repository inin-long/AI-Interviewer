# 数据库设计

## 概述

AI Interviewer 使用 **SQLite** 作为本地数据库，通过 **Flyway** 管理 schema 迁移，**MyBatis** 进行 SQL 映射。

- 数据库文件：`AI-Interviewer/database/app.db`
- 迁移文件：`src/main/resources/db/migration/`（V1-V14）
- 当前版本：V14

---

## 设计原则

| 原则 | 说明 |
|------|------|
| 主键 | 统一使用 `BIGINT` |
| 时间字段 | 所有业务表包含 `create_time` 和 `update_time` |
| 软删除 | `deleted INTEGER DEFAULT 0` |
| 枚举存储 | 使用字符串（`VARCHAR(32)`），非整数 |
| 外键 | 通过应用层维护，非数据库约束 |

---

## 表结构

### 1. user 表

```sql
CREATE TABLE user (
    id            BIGINT PRIMARY KEY AUTOINCREMENT,
    username      VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    nickname      VARCHAR(64),
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted       INTEGER DEFAULT 0
);
```

### 2. resume 表

```sql
CREATE TABLE resume (
    id            BIGINT PRIMARY KEY AUTOINCREMENT,
    user_id       BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_path  VARCHAR(512) NOT NULL,
    status        VARCHAR(32) DEFAULT 'UPLOADED',
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted       INTEGER DEFAULT 0
);
```

### 3. candidate_profile 表

```sql
CREATE TABLE candidate_profile (
    id            BIGINT PRIMARY KEY AUTOINCREMENT,
    resume_id     BIGINT NOT NULL,
    user_id       BIGINT NOT NULL,
    content_json  TEXT,
    status        VARCHAR(32) DEFAULT 'PENDING',
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted       INTEGER DEFAULT 0
);
```

### 4. interview_plan 表

```sql
CREATE TABLE interview_plan (
    id            BIGINT PRIMARY KEY AUTOINCREMENT,
    user_id       BIGINT NOT NULL,
    title         VARCHAR(128) NOT NULL,
    job_title     VARCHAR(128),
    job_description TEXT,
    difficulty    VARCHAR(32) DEFAULT 'MEDIUM',
    duration_minutes INT,
    question_count INT,
    custom_rules  TEXT,
    resume_id     BIGINT,
    profile_id    BIGINT,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted       INTEGER DEFAULT 0
);
```

### 5. interview_session 表

```sql
CREATE TABLE interview_session (
    id            BIGINT PRIMARY KEY AUTOINCREMENT,
    user_id       BIGINT NOT NULL,
    plan_id       BIGINT,
    resume_id     BIGINT,
    title         VARCHAR(128),
    stage         VARCHAR(32) DEFAULT 'INTRODUCTION',
    status        VARCHAR(32) DEFAULT 'CREATED',
    plan_snapshot TEXT,
    profile_snapshot TEXT,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted       INTEGER DEFAULT 0
);
```

### 6. interview_message 表

```sql
CREATE TABLE interview_message (
    id            BIGINT PRIMARY KEY AUTOINCREMENT,
    session_id    BIGINT NOT NULL,
    role          VARCHAR(16) NOT NULL,
    content       TEXT NOT NULL,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 7. agent_checkpoint 表

```sql
CREATE TABLE agent_checkpoint (
    id            BIGINT PRIMARY KEY AUTOINCREMENT,
    session_id    BIGINT NOT NULL,
    node_name     VARCHAR(64),
    state_json    TEXT,
    version       INT DEFAULT 1,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 8. background_task 表

```sql
CREATE TABLE background_task (
    id            BIGINT PRIMARY KEY AUTOINCREMENT,
    user_id       BIGINT NOT NULL,
    task_type     VARCHAR(32) NOT NULL,
    status        VARCHAR(32) DEFAULT 'PENDING',
    progress      INT DEFAULT 0,
    payload_json  TEXT,
    error_message TEXT,
    deduplication_key VARCHAR(128),
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted       INTEGER DEFAULT 0
);
```

### 9. evaluation 表

```sql
CREATE TABLE evaluation (
    id            BIGINT PRIMARY KEY AUTOINCREMENT,
    session_id    BIGINT NOT NULL,
    dimension     VARCHAR(32) NOT NULL,
    score         DECIMAL(5,2),
    comment       TEXT,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 10. interview_report 表

```sql
CREATE TABLE interview_report (
    id               BIGINT PRIMARY KEY AUTOINCREMENT,
    interview_id     BIGINT NOT NULL,
    title            VARCHAR(128),
    content_markdown TEXT,
    score            DECIMAL(5,2),
    status           VARCHAR(32) DEFAULT 'GENERATING',
    create_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER DEFAULT 0
);
```

### 11. knowledge_document 表

```sql
CREATE TABLE knowledge_document (
    id            BIGINT PRIMARY KEY AUTOINCREMENT,
    user_id       BIGINT NOT NULL,
    name          VARCHAR(255) NOT NULL,
    storage_path  VARCHAR(512) NOT NULL,
    status        VARCHAR(32) DEFAULT 'UPLOADED',
    chunk_count   INT DEFAULT 0,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted       INTEGER DEFAULT 0
);
```

### 12. document_chunk 表

```sql
CREATE TABLE document_chunk (
    id            BIGINT PRIMARY KEY AUTOINCREMENT,
    document_id   BIGINT NOT NULL,
    chunk_index   INT NOT NULL,
    content       TEXT NOT NULL,
    token_count   INT,
    vector_id     VARCHAR(128),
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 数据关系图

```
┌──────────┐     ┌──────────┐     ┌──────────────────┐
│   user   │────→│  resume  │────→│ candidate_profile │
└──────────┘     └──────────┘     └──────────────────┘
     │
     ├──────────────────────────────────────────┐
     │                                          │
     ▼                                          ▼
┌──────────────────┐              ┌──────────────────┐
│ interview_session │              │ knowledge_document│
└──────────────────┘              └──────────────────┘
     │                                    │
     ├──── interview_message              ├──── document_chunk
     ├──── agent_checkpoint               │
     ├──── interview_report               │
     └──── evaluation                     │
                                          │
┌──────────────────┐                      │
│ background_task  │                      │
└──────────────────┘                      │
                                          │
┌──────────────────┐                      │
│ interview_plan   │                      │
└──────────────────┘                      │
```

---

## MyBatis Mapper

共 11 个 Mapper 接口：

| Mapper | 对应表 |
|--------|--------|
| `UserMapper` | user |
| `ResumeMapper` | resume |
| `CandidateProfileMapper` | candidate_profile |
| `InterviewPlanMapper` | interview_plan |
| `InterviewSessionMapper` | interview_session |
| `InterviewMessageMapper` | interview_message |
| `AgentCheckpointMapper` | agent_checkpoint |
| `BackgroundTaskMapper` | background_task |
| `InterviewResultMapper` | evaluation |
| `InterviewPlanDocumentMapper` | interview_plan 关联 |
| `KnowledgeDocumentMapper` | knowledge_document |

---

## 迁移历史

| 版本 | 说明 |
|------|------|
| V1 | 创建 user 表 |
| V2 | 创建 resume 表 |
| V3 | 创建 candidate_profile 表 |
| V4 | 创建 interview_session 表 |
| V5 | 创建 interview_message 表 |
| V6 | 创建 agent_checkpoint 表 |
| V7 | 创建 background_task 表 |
| V8 | 创建 evaluation 表 |
| V9 | 创建 interview_report 表 |
| V10 | 创建 knowledge_document 表 |
| V11 | 创建 document_chunk 表 |
| V12 | 创建 interview_plan 表 |
| V13 | 添加会话快照字段 |
| V14 | 添加任务去重键 |
