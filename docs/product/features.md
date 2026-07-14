# 核心功能

## 功能模块总览

AI Interviewer 包含以下核心功能模块：

```
┌─────────────────────────────────────────────────┐
│                  AI Interviewer                  │
├─────────┬─────────┬─────────┬─────────┬─────────┤
│ 用户系统 │ 简历管理 │ 知识库  │ 面试系统 │ 报告系统 │
└─────────┴─────────┴─────────┴─────────┴─────────┘
```

---

## 用户系统

### 功能

- 本地多用户注册/登录
- BCrypt 密码加密存储
- 用户间数据严格隔离

### 技术实现

- `UserService`：用户注册、登录验证、事件发布
- `SecurityConfiguration`：BCrypt PasswordEncoder
- 每个用户拥有独立的文件存储目录

---

## 简历管理

### 功能

- 支持 PDF、DOCX、Markdown、TXT 格式上传
- 最大文件大小：20MB
- 后台异步解析，不阻塞 UI
- 自动生成候选人画像

### 工作流程

```
上传简历 → ResumeService 存储文件
    ↓
创建后台任务 → ResumeTaskService 入队
    ↓
BackgroundTaskWorker 执行 → Tika 提取文本
    ↓
AI 分析生成 CandidateProfile
    ↓
用户确认画像 → 进入面试准备
```

### 关键类

| 类 | 职责 |
|---|---|
| `ResumeService` | 简历 CRUD、Tika 文本提取 |
| `ResumeTaskService` | 后台解析任务入队 |
| `ResumeParseTaskHandler` | 执行简历解析逻辑 |
| `CandidateProfileService` | 画像 CRUD、人工确认流程 |
| `LocalCandidateProfileExtractor` | 本地草稿模式（无 AI） |

---

## 知识库

### 功能

- 上传技术文档、笔记、项目资料
- 自动文本分块和向量化
- 基于 Lucene 的向量相似度搜索
- 面试时自动检索相关知识

### RAG 处理管道

```
文档上传 → DocumentParser (Tika) 解析
    ↓
DocumentChunker 分块 (1000 tokens, 100 overlap)
    ↓
EmbeddingService 向量化 (OpenAI-compatible API)
    ↓
LuceneVectorStore 存储 (per-user 索引目录)
    ↓
面试时 → VectorStorePort 相似度搜索 → AgentTool 供 Agent 使用
```

### 关键类

| 类 | 职责 |
|---|---|
| `KnowledgeDocumentService` | 文档上传、解析、Embedding、索引 |
| `TikaDocumentParser` | Apache Tika 文档解析 |
| `DocumentChunker` | 文本分块（可配置大小和重叠） |
| `OpenAiEmbeddingService` | 文本向量化 |
| `LuceneVectorStore` | KNN 向量存储和搜索 |
| `KnowledgeSearchTool` | Agent 工具，检索相关知识块 |

---

## 面试系统

### 功能

- 创建面试方案（关联简历、画像、知识库）
- AI 自动生成面试问题
- 流式输出面试过程
- 智能追问和阶段转换
- 面试暂停和断点恢复
- Checkpoint 状态持久化

### 面试阶段

```
INTRODUCTION → RESUME_REVIEW → PROJECT_EXPERIENCE
      ↓
TECHNICAL_DEEP_DIVE → SYSTEM_DESIGN → CODING
      ↓
BEHAVIORAL → SUMMARY → COMPLETED
```

### Agent 状态机

基于 LangGraph4j 的有向图：

```
┌──────────────┐     ┌──────────────────┐     ┌─────────────────┐
│ AnswerAnalyzer│────→│ FollowUpDecision │────→│ StageTransition │
│    (Node)    │     │     (Node)      │     │     (Node)      │
└──────────────┘     └──────────────────┘     └─────────────────┘
                                                    ↓
                     ┌──────────────────┐     ┌─────────────────┐
                     │       END        │←────│QuestionGenerator│
                     │                  │     │     (Node)      │
                     └──────────────────┘     └─────────────────┘
```

### 关键类

| 类 | 职责 |
|---|---|
| `InterviewGraph` | 构建和编译 LangGraph4j 状态图 |
| `AnswerAnalyzerNode` | 分析回答正确性、深度、遗漏点 |
| `FollowUpDecisionNode` | 决定是否追问或推进阶段 |
| `StageTransitionNode` | 验证和执行阶段转换 |
| `QuestionGeneratorNode` | 构建问题提示并流式生成问题 |
| `InterviewAgentService` | 编排整个 AI 面试流程 |
| `StageManager` | 管理有效的阶段转换规则 |

### Agent 工具

| 工具 | 用途 |
|---|---|
| `KnowledgeSearchTool` | 通过 Lucene 向量搜索检索相关知识块 |
| `CandidateProfileTool` | 返回当前会话的候选人画像快照 |

---

## 报告系统

### 功能

- 面试完成后自动生成评估报告
- Markdown 格式，包含评分和详细分析
- 报告与面试记录关联
- 支持历史记录查看

### 报告内容

1. 综合评分
2. 技术能力分析
3. 优势总结
4. 不足之处
5. 改进建议
6. 学习方向

### 关键类

| 类 | 职责 |
|---|---|
| `ReportGenerationTaskService` | 报告生成任务入队和跟踪 |
| `ReportGenerationTaskHandler` | 执行报告生成逻辑 |
| `InterviewResultService` | 评估和评分持久化 |
| `InterviewHistoryService` | 历史面试记录查询 |

---

## 后台任务系统

### 功能

- SQLite 持久化任务队列
- 原子性任务领取
- 可配置重试策略
- 启动时自动恢复中断任务
- 全局任务中心实时反馈

### 任务类型

| 类型 | 说明 |
|---|---|
| RESUME_PARSE | 简历解析 |
| DOCUMENT_PARSE | 知识文档解析 |
| EMBEDDING_GENERATE | 向量生成 |
| VECTOR_UPDATE | 向量索引更新 |
| CANDIDATE_PROFILE | 候选人画像生成 |
| REPORT_GENERATION | 报告生成 |

### 关键类

| 类 | 职责 |
|---|---|
| `BackgroundTaskService` | 任务队列 CRUD、入队、领取、执行、重试 |
| `BackgroundTaskWorker` | SmartLifecycle 线程池，轮询任务队列 |
| `BackgroundTaskHandlerRegistry` | 策略模式，按类型分发任务处理 |
