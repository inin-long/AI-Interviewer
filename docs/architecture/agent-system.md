# Agent 系统

## 概述

AI Interviewer 的核心智能系统采用 **LangGraph4j** 构建，实现为一个有向状态图。

> **单 Agent + 状态机流程控制架构**

核心思想：

```
固定面试流程 + AI 动态决策 + 规则约束 = 可控 AI 面试
```

---

## 架构图

```
                    ┌─────────────────────┐
                    │   InterviewGraph    │
                    │  (LangGraph4j)     │
                    └─────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
              ▼               ▼               ▼
    ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
    │  Entry Point │ │  Condition   │ │   END        │
    │  plan()      │ │  Edges       │ │              │
    └──────────────┘ └──────────────┘ └──────────────┘
              │
              ▼
    ┌──────────────────────────────────────────────┐
    │                                              │
    │  ┌────────────┐   ┌────────────────┐        │
    │  │  Answer    │──→│  FollowUp      │        │
    │  │  Analyzer  │   │  Decision      │        │
    │  └────────────┘   └────────────────┘        │
    │                          │                   │
    │                    ┌─────┴─────┐             │
    │                    │           │             │
    │                    ▼           ▼             │
    │           ┌────────────┐ ┌────────────┐     │
    │           │   Stage    │ │ Question   │     │
    │           │ Transition │ │ Generator  │     │
    │           └────────────┘ └────────────┘     │
    │                    │           │             │
    │                    └─────┬─────┘             │
    │                          │                   │
    └──────────────────────────┼───────────────────┘
                               │
                               ▼
                          ┌────────┐
                          │  END   │
                          └────────┘
```

---

## 状态图定义

### InterviewGraph

负责构建和编译 LangGraph4j 状态图。

```java
// 图结构
answer_analysis → follow_up_decision → (条件分支)
                                         ├── stage_transition → END
                                         └── question_generator → END
```

### 入口点

| 方法 | 用途 |
|------|------|
| `plan()` | 执行完整面试轮次（分析 → 决策 → 转换/提问） |
| `initialQuestionPrompt()` | 生成初始面试问题 |

---

## 处理节点

### 1. AnswerAnalyzerNode

**职责**：调用 AI 分析候选人的回答

**输入**：
- 候选人回答内容
- 当前面试阶段
- 候选人画像
- 相关知识（RAG 检索结果）

**输出**：
- 回答正确性评估
- 深度分析
- 遗漏要点
- 建议追问方向

### 2. FollowUpDecisionNode

**职责**：调用 AI 决定是否追问

**决策逻辑**：
- 回答浅显 → 追问深入
- 回答充分 → 推进到下一阶段
- 回答偏离 → 引导回正题
- 阶段时间到 → 强制转换

### 3. StageTransitionNode

**职责**：验证和执行阶段转换

**规则**：
- 遵循 `StageManager` 定义的有效转换路径
- 不允许跳跃阶段（除特定规则外）
- 记录阶段转换日志

### 4. QuestionGeneratorNode

**职责**：构建问题提示并流式生成问题

**流程**：
1. 收集上下文（画像、知识、历史问答）
2. 构建提示词（使用 `AgentPrompts` 模板）
3. 调用 AI 流式生成问题
4. 返回生成的问题文本

---

## 面试阶段

### 阶段定义

| 阶段 | 英文 | 说明 |
|------|------|------|
| 开场介绍 | INTRODUCTION | 候选人自我介绍 |
| 简历回顾 | RESUME_REVIEW | 基于简历提问 |
| 项目经验 | PROJECT_EXPERIENCE | 深入项目细节 |
| 技术深入 | TECHNICAL_DEEP_DIVE | 技术细节追问 |
| 系统设计 | SYSTEM_DESIGN | 架构设计能力 |
| 编码 | CODING | 编程能力 |
| 行为面试 | BEHAVIORAL | 软技能评估 |
| 总结 | SUMMARY | 面试总结 |
| 完成 | COMPLETED | 面试结束 |

### 阶段管理

`StageManager` 负责管理有效的阶段转换规则：

```
INTRODUCTION → RESUME_REVIEW → PROJECT_EXPERIENCE
      ↓
TECHNICAL_DEEP_DIVE → SYSTEM_DESIGN → CODING
      ↓
BEHAVIORAL → SUMMARY → COMPLETED
```

---

## 状态管理

### InterviewGraphState

管理图执行过程中的状态数据：

| 字段 | 类型 | 说明 |
|------|------|------|
| sessionId | String | 当前会话 ID |
| userId | String | 当前用户 ID |
| currentStage | InterviewStage | 当前面试阶段 |
| messages | `List<Message>` | 对话历史 |
| candidateProfile | `CandidateProfile` | 候选人画像 |
| knowledgeChunks | `List<VectorSearchResult>` | RAG 检索结果 |
| lastAnswer | String | 最新回答 |
| analysisResult | AnswerAnalysis | 分析结果 |
| followUpDecision | AgentDecision | 追问决策 |
| generatedQuestion | String | 生成的问题 |

### Checkpoint 机制

**JacksonStateSerializer** 负责状态的序列化和反序列化：

- 每个节点执行后保存 Checkpoint
- Checkpoint 存储在 `agent_checkpoint` 表
- 支持从任意 Checkpoint 恢复
- 面试暂停时保存完整状态

---

## Agent 工具

### ToolRegistry

工具注册中心，所有 Agent 工具通过 `ToolRegistry` 注册和管理。

### 工具列表

#### KnowledgeSearchTool

**用途**：通过 Lucene 向量搜索检索相关知识块

**输入**：查询文本
**输出**：相关的知识文档片段

**流程**：
1. 接收查询文本
2. 调用 `EmbeddingService` 生成查询向量
3. 通过 `VectorStorePort` 执行 KNN 搜索
4. 返回 Top-K 相关片段

#### CandidateProfileTool

**用途**：返回当前会话的候选人画像快照

**输入**：会话 ID
**输出**：冻结的候选人画像 JSON

**说明**：面试开始时画像被冻结为快照，后续编辑不影响历史会话。

---

## 提示词管理

### AgentPrompts

静态提示词模板类，包含：

| 模板 | 用途 |
|------|------|
| 分析提示词 | 指导 AI 分析回答质量 |
| 决策提示词 | 指导 AI 决定追问/推进 |
| 问题生成提示词 | 指导 AI 生成面试问题 |

所有提示词为中文，针对技术面试场景优化。

---

## 数据流

```
用户提交回答
    ↓
InterviewAgentService 接收
    ↓
构建 InterviewTurnInput
    ↓
调用 InterviewGraph.plan()
    ↓
┌─→ AnswerAnalyzerNode (分析回答)
│       ↓
│   FollowUpDecisionNode (决策)
│       ↓
│   ┌───┴───┐
│   │       │
│   ▼       ▼
│ Stage    Question
│ Transition Generator
│   │       │
│   └───┬───┘
│       ↓
└─── 保存 Checkpoint
    ↓
返回 AgentAction
    ↓
InterviewAgentService 处理结果
    ↓
保存消息到数据库
    ↓
流式输出给 UI
```

---

## 实现类

| 类 | 位置 | 职责 |
|---|---|---|
| `InterviewGraph` | `agent/graph/` | 构建和编译状态图 |
| `AnswerAnalyzerNode` | `agent/node/` | 分析回答 |
| `FollowUpDecisionNode` | `agent/node/` | 决策追问/推进 |
| `StageTransitionNode` | `agent/node/` | 阶段转换 |
| `QuestionGeneratorNode` | `agent/node/` | 生成问题 |
| `InterviewGraphState` | `agent/state/` | 图状态管理 |
| `StageManager` | `agent/stage/` | 阶段转换规则 |
| `AgentPrompts` | `agent/prompt/` | 提示词模板 |
| `ToolRegistry` | `agent/tool/` | 工具注册中心 |
| `KnowledgeSearchTool` | `agent/tool/` | 知识检索工具 |
| `CandidateProfileTool` | `agent/tool/` | 画像查询工具 |
