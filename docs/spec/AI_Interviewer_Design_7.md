

# 第七部分：附录——开发实施规范、核心类列表、数据库迁移规划、MVP 实现顺序

本部分用于指导编码 Agent 按照正确顺序实现项目。

目标：

避免低智能模型：

- 先写 UI 后补业务
- 随意改变架构
- 创建重复代码
- 跳过基础设施建设
- 一次性生成不可维护代码

------

# 75. MVP 开发原则

## 75.1 开发顺序原则

必须按照：

```
基础设施

↓

数据层

↓

业务层

↓

Agent层

↓

UI层

↓

优化
```

顺序开发。

------

禁止：

先开发：

```
❌ AI聊天窗口

❌ UI动画

❌ 报告页面

❌ 高级功能
```

再补：

数据库和架构。

------

# 76. MVP 实现阶段规划

------

# Phase 1：项目基础初始化

目标：

建立可运行工程。

------

实现：

## Maven

完成：

```
pom.xml
```

依赖：

- Spring Boot
- JavaFX
- MyBatis
- SQLite
- Flyway
- Spring AI
- LangGraph4j

------

## Spring Boot 启动

完成：

```
ApplicationLauncher
```

要求：

Spring Context 正常启动。

------

## JavaFX 集成

完成：

- MainWindow
- FXML加载
- Controller注入

------

验收：

启动程序：

显示：

```
空白主窗口
```

------

# Phase 2：数据库与用户系统

目标：

完成本地用户基础能力。

------

实现：

## Flyway

创建：

```
V1__init.sql
```

------

创建表：

- user

------

实现：

```
UserEntity

UserMapper

UserService
```

------

实现：

- 注册
- 登录
- BCrypt验证

------

验收：

用户：

```
注册

↓

登录

↓

进入主页
```

------

# Phase 3：文件系统

目标：

建立用户数据隔离。

------

实现：

```
PathService

FileStorageService
```

------

创建目录：

```
AI-Interviewer/

└── users/

    └── user-id/
```

------

验收：

上传文件：

自动保存：

```
users/{id}/
```

------

# Phase 4：简历系统

目标：

完成用户画像基础。

------

数据库：

增加：

```
V2__add_resume.sql
```

------

实现：

表：

- resume
- candidate_profile

------

功能：

- 上传简历
- 文件保存
- Tika解析
- AI提取 Profile

------

流程：

```
Upload

↓

Parse

↓

AI Extract

↓

CandidateProfile
```

------

验收：

上传简历：

生成：

```
CandidateProfile
```

------

# Phase 5：Agent 基础框架

目标：

建立 LangGraph。

------

实现：

## State

```
InterviewState
```

------

## Graph

```
InterviewGraph
```

------

## Node

先实现：

```
QuestionGeneratorNode
```

------

暂时：

不实现：

- 复杂评分
- RAG
- Report

------

验收：

启动：

Agent 可以：

生成一个问题。

------

# Phase 6：完整面试流程

目标：

完成核心产品。

------

增加：

Stage：

```
INTRODUCTION

PROJECT

TECHNICAL

SUMMARY
```

------

实现：

Node：

```
QuestionGenerator

AnswerAnalyzer

Decision

StageTransition
```

------

支持：

- 提问
- 回答
- 追问
- 阶段切换

------

验收：

完成一次完整模拟面试。

------

# Phase 7：Checkpoint 恢复

目标：

支持暂停恢复。

------

实现：

数据库：

```
agent_checkpoint
```

------

流程：

```
Pause

↓

Save State


Resume

↓

Restore State
```

------

验收：

关闭程序：

重新打开：

继续面试。

------

# Phase 8：RAG 知识库

目标：

增加知识增强。

------

实现：

数据库：

```
document

document_chunk
```

------

实现：

流程：

```
Upload

↓

Tika

↓

Chunk

↓

Embedding

↓

Vector Store
```

------

实现 Tool：

```
KnowledgeSearchTool
```

------

验收：

Agent 可以查询用户知识库。

------

# Phase 9：评分与报告

目标：

完成完整闭环。

------

实现：

EvaluationNode：

输出：

```
EvaluationResult
```

------

实现：

ReportGeneratorNode：

输出：

Markdown。

------

数据库：

增加：

```
report
```

------

验收：

面试结束：

自动生成报告。

------

# Phase 10：UI 完善

目标：

提升用户体验。

------

实现：

页面：

- Dashboard
- Resume
- Profile
- Interview
- Knowledge
- History
- Report
- Settings

------

实现：

组件：

- AppButton
- AppCard
- LoadingView
- ErrorView
- MarkdownView

------

# 77. 核心类列表

以下类必须存在。

------

# 77.1 Application

```
ApplicationLauncher
```

职责：

程序入口。

------

# 77.2 Configuration

```
AppProperties

LLMProperties

StorageProperties

TaskProperties

ConfigValidator
```

------

# 77.3 User

```
UserEntity

UserMapper

UserService

UserController
```

------

# 77.4 Resume

```
ResumeEntity

ResumeMapper

ResumeService

ResumeParser
```

------

# 77.5 Interview

```
InterviewSessionEntity

InterviewService

InterviewController
```

------

# 77.6 Agent

核心：

```
InterviewAgent

InterviewGraph

InterviewState
```

------

Node：

```
QuestionGeneratorNode

AnswerAnalyzerNode

FollowUpDecisionNode

StageTransitionNode

EvaluationNode

ReportGeneratorNode
```

------

# 77.7 Tool

必须：

```
ToolRegistry

ProfileQueryTool

KnowledgeSearchTool

HistoryQueryTool

RuleQueryTool
```

------

# 77.8 AI

```
ChatService

EmbeddingService

RetryService
```

------

# 77.9 Task

```
TaskService

TaskWorker

TaskExecutor
```

------

# 78. 数据库迁移规划

最终：

```
resources/db/migration/
```

------

# V1 初始化

文件：

```
V1__init.sql
```

创建：

```
user

interview_session

message
```

------

# V2 简历系统

文件：

```
V2__add_resume.sql
```

创建：

```
resume

candidate_profile
```

------

# V3 Agent恢复

文件：

```
V3__add_checkpoint.sql
```

创建：

```
agent_checkpoint
```

------

# V4 知识库

文件：

```
V4__add_knowledge.sql
```

创建：

```
document

document_chunk
```

------

# V5 任务系统

文件：

```
V5__add_task.sql
```

创建：

```
task
```

------

# V6 报告系统

文件：

```
V6__add_report.sql
```

创建：

```
report

evaluation
```

------

# 79. 开发验收标准

------

## 基础运行

必须：

```
程序可以启动

Spring正常加载

JavaFX正常显示
```

------

## 用户系统

必须：

```
注册成功

登录成功

数据隔离
```

------

## AI系统

必须：

```
调用LLM成功

Agent生成问题

完成面试流程
```

------

## 数据安全

必须：

```
密码Hash

用户隔离

API Key保护
```

------

## 稳定性

必须：

```
异常可捕获

日志存在

任务可恢复
```

------

# 80. 最终架构总览

完整系统：

```
                         User

                          │

                          ↓


                 ┌────────────────┐
                 │    JavaFX UI   │
                 └────────────────┘

                          │

                          ↓


                 ┌────────────────┐
                 │ Spring Boot    │
                 │ Application    │
                 └────────────────┘

                          │


        ┌─────────────────┼─────────────────┐


        ↓                 ↓                 ↓


   Database          Agent System       Task System


   SQLite            LangGraph4j        Worker


        ↓                 ↓                 ↓


   File System       Spring AI         Vector Store
```

------

# 81. 项目最终约束总结

编码 Agent 必须遵守：

------

## 技术约束

固定：

```
Java

JavaFX

Spring Boot

SQLite

MyBatis

Flyway

Spring AI

LangGraph4j
```

------

## 架构约束

固定：

```
单进程

单Maven Module

分层架构

本地多用户
```

------

## AI约束

固定：

```
单Agent

固定Stage

Tool Registry

Rule + Agent Decision
```

------

## 数据约束

固定：

```
SQLite

用户隔离

文件系统存储

Checkpoint恢复
```

------

## UI约束

固定：

```
单窗口

ViewManager

Component Library

CSS Design System
```