# 后台任务系统

## 概述

AI Interviewer 使用 SQLite 持久化的后台任务队列，处理耗时的异步操作。任务在后台线程池中执行，支持自动重试和重启恢复。

---

## 架构

```
┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│  业务 Service    │────→│ BackgroundTask   │────→│   SQLite 数据库   │
│  (入队任务)      │     │    Service       │     │  (任务持久化)     │
└──────────────────┘     └──────────────────┘     └──────────────────┘
                                │                          │
                                ▼                          ▼
                         ┌──────────────────┐     ┌──────────────────┐
                         │ BackgroundTask   │←────│ BackgroundTask   │
                         │    Worker        │     │    Worker        │
                         │  (轮询执行)      │     │  (启动恢复)      │
                         └──────────────────┘     └──────────────────┘
                                │
                                ▼
                         ┌──────────────────┐
                         │  TaskHandler     │
                         │  Registry        │
                         │  (策略分发)      │
                         └──────────────────┘
                                │
                    ┌───────────┼───────────┐
                    │           │           │
                    ▼           ▼           ▼
              ┌──────────┐ ┌──────────┐ ┌──────────┐
              │ Resume   │ │ Knowledge│ │ Report   │
              │ Parse    │ │ Document │ │ Generation│
              │ Handler  │ │ Handler  │ │ Handler  │
              └──────────┘ └──────────┘ └──────────┘
```

---

## 任务类型

| 类型 | 枚举值 | 说明 |
|------|--------|------|
| 简历解析 | `RESUME_PARSE` | 解析上传的简历文件 |
| 知识文档处理 | `DOCUMENT_PARSE` | 解析、分块、Embedding 知识文档 |
| 向量生成 | `EMBEDDING_GENERATE` | 生成文本向量 |
| 向量索引更新 | `VECTOR_UPDATE` | 更新 Lucene 向量索引 |
| 候选人画像 | `CANDIDATE_PROFILE` | 生成候选人画像 |
| 报告生成 | `REPORT_GENERATION` | 生成面试评估报告 |

---

## 任务生命周期

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│ PENDING │───→│ CLAIMED │───→│ RUNNING │───→│COMPLETED│    │         │
└─────────┘    └─────────┘    └─────────┘    └─────────┘    │         │
                        │            │                       │         │
                        │            ▼                       │         │
                        │       ┌─────────┐                  │         │
                        │       │ FAILED  │───→ 重试 ──→ PENDING     │
                        │       └─────────┘                  │         │
                        │                                    │         │
                        └──── 启动恢复 ──────────────────────┘         │
                                                                      │
                    任务去重键 (deduplication_key) ────────────────────┘
```

### 状态说明

| 状态 | 说明 |
|------|------|
| `PENDING` | 等待执行 |
| `CLAIMED` | 已被 Worker 领取 |
| `RUNNING` | 正在执行 |
| `COMPLETED` | 执行成功 |
| `FAILED` | 执行失败（可重试） |

---

## 核心组件

### BackgroundTaskService

任务队列的核心服务，提供以下能力：

| 方法 | 说明 |
|------|------|
| `enqueue()` | 入队新任务 |
| `claim()` | 原子性领取任务 |
| `execute()` | 执行任务 |
| `retry()` | 重试失败任务 |
| `recover()` | 恢复中断任务 |

### BackgroundTaskWorker

基于 Spring `SmartLifecycle` 的后台工作线程：

| 配置 | 默认值 | 说明 |
|------|--------|------|
| workerCount | 2 | 工作线程数 |
| pollInterval | 3000ms | 轮询间隔 |
| retryCount | 3 | 最大重试次数 |
| retryDelay | 5000ms | 重试延迟 |

**启动恢复**：应用启动时自动恢复之前中断的任务。

### BackgroundTaskHandlerRegistry

策略模式实现，按任务类型分发到对应的 Handler：

```java
`Map<BackgroundTaskType, BackgroundTaskHandler> handlers;`
```

---

## 任务处理器

### ResumeParseTaskHandler

**职责**：解析简历文件

**流程**：
1. 读取简历文件
2. Tika 提取文本
3. 更新简历状态为 COMPLETED
4. （可选）触发画像生成任务

### KnowledgeDocumentTaskHandler

**职责**：处理知识文档

**流程**：
1. Tika 解析文档
2. DocumentChunker 分块
3. EmbeddingService 向量化
4. LuceneVectorStore 索引
5. 更新文档状态和 chunk_count

### CandidateProfileTaskHandler

**职责**：生成候选人画像

**流程**：
1. 读取简历文本
2. 调用 AI 分析生成画像
3. 保存 CandidateProfile
4. 发布完成事件

### ReportGenerationTaskHandler

**职责**：生成面试评估报告

**流程**：
1. 读取面试对话历史
2. 调用 AI 生成评估报告
3. 保存 Markdown 报告
4. 更新报告状态

---

## 任务去重

通过 `deduplication_key` 实现任务去重：

- 每个任务可以关联一个唯一键
- 入队时检查是否已存在相同键的待执行任务
- 避免重复处理同一文档或简历

---

## 错误处理与重试

### 重试策略

| 参数 | 值 | 说明 |
|------|-----|------|
| 最大重试次数 | 3 | 可通过配置调整 |
| 重试延迟 | 5000ms | 指数退避 |
| 重试条件 | 临时性错误 | 网络超时、API 限流等 |

### 失败处理

1. 任务标记为 FAILED
2. 记录错误信息到 `error_message` 字段
3. 用户可通过任务中心手动重试
4. 超过重试次数后需要人工介入

---

## 事件驱动通知

任务状态变更通过 Spring ApplicationEvent 发布：

| 事件 | 触发时机 |
|------|----------|
| `BackgroundTaskCreatedEvent` | 任务入队成功 |
| `BackgroundTaskCompletedEvent` | 任务执行成功 |
| `BackgroundTaskFailedEvent` | 任务执行失败 |
| `BackgroundTaskRetriedEvent` | 任务重试 |

UI 层通过 `TaskNotificationCenter` 订阅这些事件，实现任务状态的实时更新。

---

## 关键类

| 类 | 位置 | 职责 |
|---|---|---|
| `BackgroundTaskService` | `application/service/` | 任务队列 CRUD |
| `BackgroundTaskWorker` | `infrastructure/task/` | 后台工作线程 |
| `BackgroundTaskHandler` | `application/task/` | 任务处理器接口 |
| `BackgroundTaskHandlerRegistry` | `application/task/` | 策略分发 |
| `ResumeParseTaskHandler` | `application/task/` | 简历解析 |
| `KnowledgeDocumentTaskHandler` | `application/task/` | 知识文档处理 |
| `CandidateProfileTaskHandler` | `application/task/` | 画像生成 |
| `ReportGenerationTaskHandler` | `application/task/` | 报告生成 |
| `BackgroundTaskMapper` | `infrastructure/database/mapper/` | 数据库操作 |
