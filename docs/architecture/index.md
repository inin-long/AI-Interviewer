# 架构总览

## 系统定位

AI Interviewer 采用 **Spring Boot + JavaFX 单进程架构**。

- 无 Web 服务器（`spring.main.web-application-type: none`）
- Spring Boot 管理所有 Bean 和生命周期
- JavaFX 提供桌面用户界面
- SQLite 作为本地数据库

---

## 整体架构图

```
┌─────────────────────────────────────────────────────────┐
│                    JavaFX UI 层                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐  │
│  │ FXML 视图 │ │Controller│ │  组件库   │ │  导航系统   │  │
│  └──────────┘ └──────────┘ └──────────┘ └────────────┘  │
├─────────────────────────────────────────────────────────┤
│                  Application 服务层                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐  │
│  │ 业务服务   │ │   DTO    │ │   事件    │ │   异常处理  │  │
│  └──────────┘ └──────────┘ └──────────┘ └────────────┘  │
├─────────────────────────────────────────────────────────┤
│                    Domain 领域层                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                 │
│  │  实体类   │ │  领域模型  │ │   枚举    │                 │
│  └──────────┘ └──────────┘ └──────────┘                 │
├─────────────────────────────────────────────────────────┤
│                 Infrastructure 基础设施层                  │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌───────┐  │
│  │SQLite  │ │ 文件系统 │ │  AI    │ │ 向量存储 │ │ 后台任务│  │
│  │MyBatis │ │  Tika  │ │SpringAI│ │ Lucene │ │Worker │  │
│  └────────┘ └────────┘ └────────┘ └────────┘ └───────┘  │
├─────────────────────────────────────────────────────────┤
│                    Agent 智能层                           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐  │
│  │ 状态图    │ │ 处理节点   │ │   工具    │ │   提示词    │  │
│  │LangGraph │ │  4 Nodes │ │ 2 Tools  │ │  Prompts  │  │
│  └──────────┘ └──────────┘ └──────────┘ └────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## 包结构

```
com.inin.aiinterviewer/
├── ApplicationLauncher.java          # JVM 入口
├── JavaFxApplication.java            # JavaFX Application
├── AiInterviewerApplication.java     # Spring Boot 主类
│
├── ui/                               # UI 层
│   ├── controller/   (18 个)         # 页面控制器
│   ├── component/                     # 可复用组件
│   ├── navigation/                    # 路由和导航
│   ├── state/                         # 会话状态
│   └── dialog/                        # 文件对话框
│
├── application/                      # 应用服务层
│   ├── service/     (16 个)          # 业务服务
│   ├── dto/         (21 个)          # 数据传输对象
│   ├── event/       (6 个)           # 应用事件
│   ├── exception/   (9 个)           # 异常定义
│   ├── mapper/                        # 实体映射
│   └── task/        (7 个)           # 任务处理器
│
├── domain/                           # 领域层
│   ├── entity/      (12 个)          # 数据库实体
│   ├── enums/       (11 个)          # 状态枚举
│   └── model/       (5 个)           # 领域模型
│
├── infrastructure/                   # 基础设施层
│   ├── ai/                          # LLM 服务
│   ├── database/mapper/ (11 个)     # MyBatis 映射器
│   ├── document/                    # 文档解析
│   ├── file/                        # 文件存储
│   ├── vector/                      # 向量存储
│   └── task/                        # 后台任务
│
├── agent/                            # AI Agent 层
│   ├── graph/                       # 状态图
│   ├── node/        (4 个)          # 处理节点
│   ├── model/                       # Agent 数据模型
│   ├── state/                       # 状态管理
│   ├── stage/                       # 阶段管理
│   ├── prompt/                      # 提示词模板
│   ├── tool/        (2 个)          # Agent 工具
│   └── support/                     # 辅助工具
│
└── config/                           # 配置类
    ├── AppProperties
    ├── LlmProperties
    ├── StorageProperties
    ├── TaskProperties
    ├── RagProperties
    └── ...
```

---

## 核心设计原则

### 1. 简单优先

避免过度工程化、微服务、不必要的抽象。单 Maven Module，单进程运行。

### 2. 分层清晰

UI → Application → Domain → Infrastructure。UI 层禁止直接访问数据库或 AI 客户端。

### 3. 可控 Agent

AI 不能修改系统规则、面试流程或创建非法状态。所有决策通过 Validator 后才执行。

### 4. 本地数据

所有用户数据存储在本地 `AI-Interviewer/` 目录，无需联网。

---

## 关键技术选型

| 技术 | 选型 | 理由 |
|------|------|------|
| UI 框架 | JavaFX | 原生桌面体验、FXML 声明式布局、CSS 样式分离 |
| 应用框架 | Spring Boot | Bean 管理、服务层、配置管理、生命周期 |
| 数据库 | SQLite | 零安装、单文件、适合本地应用 |
| SQL 映射 | MyBatis | 灵活的 SQL 控制，适合 SQLite 特性 |
| 数据库迁移 | Flyway | 版本化 schema 管理 |
| AI 集成 | Spring AI | OpenAI 兼容 API、流式输出 |
| Agent | LangGraph4j | 状态机流程控制、Checkpoint、条件分支 |
| 文档解析 | Apache Tika | 支持多格式、成熟稳定 |
| 向量存储 | Apache Lucene | KNN 搜索、per-user 索引 |
| 测试 | JUnit5 + Mockito + TestFX | 单元测试 + 集成测试 + UI 测试 |
