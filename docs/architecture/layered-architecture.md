# 分层架构

## 架构概览

AI Interviewer 采用经典的分层架构，共五层：

```
┌─────────────────────────────────────┐
│         UI 层 (JavaFX)              │
│   FXML + Controller + Component     │
├─────────────────────────────────────┤
│       Application 服务层             │
│   Service + DTO + Event + Exception │
├─────────────────────────────────────┤
│         Domain 领域层                │
│      Entity + Model + Enum          │
├─────────────────────────────────────┤
│      Infrastructure 基础设施层       │
│  Database + File + AI + Vector      │
├─────────────────────────────────────┤
│         Agent 智能层                 │
│  Graph + Node + Tool + Prompt       │
└─────────────────────────────────────┘
```

---

## UI 层

### 职责

- JavaFX 页面渲染和用户交互
- UI 状态显示
- 页面导航和路由
- 输入验证（前端层面）

### 组成

| 组件 | 数量 | 说明 |
|------|------|------|
| FXML 视图 | 18 个 | 声明式布局定义 |
| Controller | 18 个 | 页面逻辑控制 |
| Component | 3 个 | 可复用 UI 组件 |
| Navigation | - | 路由枚举、ViewManager、ContentNavigator |

### 禁止事项

- ❌ 直接访问 Mapper
- ❌ 直接访问 SQLite
- ❌ 直接访问 AI Client
- ❌ 直接访问文件系统

**必须通过 Application Service 层进行所有业务操作。**

### 关键类

```
ui/
├── controller/
│   ├── LoginController              # 登录页
│   ├── RegisterController           # 注册页
│   ├── MainWindowController         # 主窗口（侧边栏、通知）
│   ├── DashboardController          # 首页仪表盘
│   ├── ResumeController             # 简历列表
│   ├── ResumeDetailController       # 简历详情
│   ├── ProfileController            # 候选人画像
│   ├── InterviewPlanController      # 面试方案列表
│   ├── InterviewPlanEditorController # 面试方案编辑
│   ├── InterviewWorkspaceController # 面试工作台（核心）
│   ├── InterviewHistoryController   # 面试历史
│   ├── InterviewHistoryDetailController # 面试详情
│   ├── InterviewReportController    # 面试报告
│   ├── KnowledgeController          # 知识库列表
│   ├── KnowledgeDetailController    # 知识库详情
│   ├── BackgroundTaskController     # 任务中心
│   ├── BackgroundTaskDetailController # 任务详情
│   └── SettingsController           # 设置页
├── component/
│   ├── InterviewTranscriptView      # 面试对话流组件
│   ├── MarkdownView                 # Markdown 渲染组件
│   └── MarkdownDocumentRenderer     # GFM Markdown 渲染器
├── navigation/
│   ├── Route                        # 路由枚举
│   ├── ViewManager                  # 视图管理接口
│   ├── JavaFxViewManager            # JavaFX 视图管理实现
│   ├── ContentNavigator             # 内容导航器
│   ├── ContextAwareController       # 上下文感知控制器接口
│   └── InterviewTranscriptContext   # 面试对话上下文
├── state/
│   ├── UserSessionState             # 用户会话状态
│   └── TaskNotificationCenter       # 任务通知中心
└── dialog/
    ├── FileDialogService            # 文件对话框接口
    └── JavaFxFileDialogService      # JavaFX 实现
```

---

## Application 服务层

### 职责

- 业务流程编排
- DTO 转换
- 事件发布
- 异常处理

### 组成

| 组件 | 数量 | 说明 |
|------|------|------|
| Service | 16 个 | 核心业务服务 |
| DTO | 21 个 | 数据传输对象 |
| Event | 6 个 | Spring 应用事件 |
| Exception | 9 个 | 异常定义和处理 |
| TaskHandler | 7 个 | 后台任务处理器 |

### 核心服务

| 服务 | 职责 |
|------|------|
| `UserService` | 用户注册、登录、BCrypt 密码、事件发布 |
| `ResumeService` | 简历 CRUD、Tika 文本提取 |
| `CandidateProfileService` | 画像 CRUD、人工确认 |
| `InterviewPlanService` | 面试方案 CRUD、关联管理 |
| `InterviewSessionService` | 会话生命周期：创建、暂停、恢复、Checkpoint |
| `InterviewAgentService` | AI 面试编排：初始问题、回答提交、RAG、图调用 |
| `InterviewCompletionService` | 最终回答保存、面试完成 |
| `InterviewResultService` | 评估和评分持久化 |
| `InterviewHistoryService` | 历史记录查询 |
| `KnowledgeDocumentService` | 知识文档处理、Embedding、索引 |
| `BackgroundTaskService` | 任务队列管理、入队、重试、恢复 |
| `ReportGenerationTaskService` | 报告生成任务入队和跟踪 |

### 事件系统

| 事件 | 触发时机 |
|------|----------|
| `UserLoggedInEvent` | 用户登录成功 |
| `InterviewTurnCompletedEvent` | 面试对话轮次完成 |
| `BackgroundTaskCreatedEvent` | 后台任务创建 |
| `BackgroundTaskCompletedEvent` | 后台任务完成 |
| `BackgroundTaskFailedEvent` | 后台任务失败 |
| `BackgroundTaskRetriedEvent` | 后台任务重试 |

### 异常体系

```
GlobalExceptionHandler
├── ErrorCode (枚举)
├── AIException              # AI 服务异常
├── BusinessException        # 业务逻辑异常
├── DataException            # 数据操作异常
├── ConfigurationException   # 配置异常
├── DocumentParseException   # 文档解析异常
├── VectorStoreException     # 向量存储异常
├── FileStorageException     # 文件存储异常
└── TaskExecutionException   # 任务执行异常
```

---

## Domain 领域层

### 职责

- 核心业务模型定义
- 独立于 Spring、JavaFX、SQLite
- 纯 Java 实现

### 组成

| 组件 | 数量 | 说明 |
|------|------|------|
| Entity | 12 个 | 数据库实体 |
| Enum | 11 个 | 状态和类型枚举 |
| Model | 5 个 | 领域模型/值对象 |

### 实体关系

```
User
  ├── Resume → CandidateProfile
  ├── InterviewSession
  │     ├── InterviewMessage
  │     ├── AgentCheckpoint
  │     └── InterviewReport
  ├── KnowledgeDocument → DocumentChunk
  └── BackgroundTask
```

### 核心枚举

| 枚举 | 值 |
|------|-----|
| `InterviewStage` | INTRODUCTION, RESUME_REVIEW, PROJECT_EXPERIENCE, TECHNICAL_DEEP_DIVE, SYSTEM_DESIGN, CODING, BEHAVIORAL, SUMMARY, COMPLETED |
| `InterviewStatus` | CREATED, RUNNING, PAUSED, COMPLETED, FAILED |
| `ResumeStatus` | UPLOADED, PARSING, COMPLETED, FAILED |
| `ProfileStatus` | PENDING, CONFIRMED |
| `BackgroundTaskStatus` | PENDING, CLAIMED, RUNNING, COMPLETED, FAILED |
| `BackgroundTaskType` | RESUME_PARSE, DOCUMENT_PARSE, EMBEDDING_GENERATE, VECTOR_UPDATE, CANDIDATE_PROFILE, REPORT_GENERATION |

---

## Infrastructure 基础设施层

### 职责

- 外部系统交互
- 技术实现细节
- 适配器模式

### 子包

| 子包 | 职责 |
|------|------|
| `database/mapper` | MyBatis Mapper 接口（11 个） |
| `ai` | LLM 服务（Spring AI） |
| `document` | 文档解析（Tika） |
| `file` | 文件存储（本地文件系统） |
| `vector` | 向量存储（Lucene） |
| `task` | 后台任务 Worker |

---

## Agent 智能层

### 职责

- AI 面试逻辑
- 状态机流程控制
- 工具调用
- 提示词管理

### 组成

| 组件 | 说明 |
|------|------|
| `InterviewGraph` | LangGraph4j 状态图构建和编译 |
| 4 个处理节点 | AnswerAnalyzer, FollowUpDecision, StageTransition, QuestionGenerator |
| 2 个 Agent 工具 | KnowledgeSearch, CandidateProfile |
| `StageManager` | 阶段转换规则管理 |
| `AgentPrompts` | 提示词模板（中文） |

详细设计见 [Agent 系统](./agent-system.md)。

---

## 层间调用规则

```
UI Controller
    ↓ (调用)
Application Service
    ↓ (调用)
Domain Model / Infrastructure
    ↓ (调用)
Agent Layer (仅 InterviewAgentService)
```

**严格禁止**：
- UI 层直接访问 Infrastructure 层
- 跨层调用（如 UI → Domain）
- 反向依赖（如 Domain → Application）
