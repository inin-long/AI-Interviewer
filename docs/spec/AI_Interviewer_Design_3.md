# 第三部分：AI Agent 系统设计

本部分定义 AI Interviewer 的核心智能系统。

目标：

明确：

- LangGraph4j 如何组织 Agent
- 面试流程如何控制
- State 如何保存
- Node 如何划分
- Tool 如何调用
- Prompt 如何管理

------

# 17. Agent 总体架构

## 17.1 Agent 定位

系统采用：

> 单 Agent + 状态机流程控制架构

不采用：

- Multi Agent
- Agent 自由规划
- Agent 自我修改流程

------

核心思想：

```
固定面试流程

        +

AI动态决策

        +

规则约束

        ↓

可控 AI 面试
```

------

## 17.2 Agent 架构

整体：

```
InterviewSession

        ↓

InterviewAgent

        ↓

LangGraph Runtime

        ↓

Node Execution

        ↓

Decision

        ↓

Next Node
```

------

详细：

```
              InterviewState

                    │

                    ↓


        ┌──────────────────┐
        │ LangGraph Engine │
        └──────────────────┘


                    │


 ┌──────────┬──────────┬──────────┬──────────┐

 ↓          ↓          ↓          ↓

Question   Analysis   Decision   Evaluation

Node       Node       Node       Node


 ↓          ↓          ↓          ↓


 AI       AI         Rule       Report
```

------

# 18. Agent Layer 目录结构

```
agent/

├── graph/

│   ├── InterviewGraph.java

│   └── GraphBuilder.java


├── node/

│   ├── QuestionGeneratorNode.java

│   ├── AnswerAnalyzerNode.java

│   ├── FollowUpDecisionNode.java

│   ├── StageTransitionNode.java

│   ├── EvaluationNode.java

│   ├── SummaryNode.java

│   └── ReportGeneratorNode.java


├── state/

│   ├── InterviewState.java

│   ├── StateSerializer.java

│   └── StateVersion.java


├── stage/

│   ├── StageManager.java

│   ├── StageDefinition.java

│   └── StageTransitionRule.java


├── tool/

│   ├── AgentTool.java

│   ├── ToolRegistry.java

│   ├── ProfileQueryTool.java

│   ├── KnowledgeSearchTool.java

│   └── HistoryQueryTool.java


└── prompt/

    ├── SystemPrompt.java

    ├── InterviewPrompt.java

    ├── AnalysisPrompt.java

    ├── DecisionPrompt.java

    ├── EvaluationPrompt.java

    └── ReportPrompt.java
```

------

# 19. LangGraph State 设计

## 19.1 State 定位

InterviewState：

表示：

> 当前一次 AI 面试运行时的完整上下文。

------

区别：

数据库：

```
InterviewSession

=
业务数据
```

------

Agent State：

```
InterviewState

=
AI运行上下文
```

------

## 19.2 InterviewState

结构：

```
public class InterviewState {


    // 当前面试ID

    Long sessionId;


    // 用户ID

    Long userId;


    // 当前阶段

    InterviewStage stage;


    // 历史消息

    List<Message> messages;


    // 当前问题

    String currentQuestion;


    // 最近回答

    String latestAnswer;


    // 当前分析结果

    AnswerAnalysis analysis;


    // 当前评分

    EvaluationResult evaluation;


    // 候选人画像

    CandidateProfile profile;


    // 面试规则

    RuleSnapshot rules;


    // 上下文摘要

    String summary;


}
```

------

# 20. State 持久化

## 20.1 保存方式

采用：

> 自定义 State Snapshot + SQLite

------

流程：

```
InterviewState

        ↓

StateSerializer

        ↓

JSON

        ↓

agent_checkpoint
```

------

## 20.2 保存时机

必须保存：

## 每轮对话完成

```
用户回答

↓

Agent分析

↓

生成下一问题

↓

保存State
```

------

## 阶段切换

例如：

```
PROJECT_EXPERIENCE

↓

TECHNICAL_DEEP_DIVE
```

保存。

------

## 用户暂停

立即保存：

```
PAUSED

↓

Checkpoint
```

------

## 20.3 恢复流程

用户点击：

```
继续面试
```

流程：

```
读取 InterviewSession

        ↓

读取最新Checkpoint

        ↓

StateSerializer反序列化

        ↓

恢复 LangGraph

        ↓

继续执行
```

------

# 21. 面试 Stage 系统

## 21.1 设计原则

采用：

> 固定 Stage + Agent 阶段内决策

------

Agent：

可以：

- 追问
- 判断回答质量
- 请求进入下一阶段

不能：

- 创建 Stage
- 修改流程
- 删除 Stage

------

## 21.2 Stage 定义

```
public enum InterviewStage {


    INTRODUCTION,


    RESUME_REVIEW,


    PROJECT_EXPERIENCE,


    TECHNICAL_DEEP_DIVE,


    SYSTEM_DESIGN,


    CODING,


    BEHAVIORAL,


    SUMMARY,


    COMPLETED

}
```

------

# 22. StageManager

负责：

控制阶段流转。

------

目录：

```
agent/stage/

├── StageManager.java

├── StageDefinition.java

└── StageTransitionRule.java
```

------

## 22.1 StageTransitionRule

示例：

```
StageTransitionRule {


    from:

    PROJECT_EXPERIENCE,


    allowedNext:

    [

      TECHNICAL_DEEP_DIVE,

      SUMMARY

    ]

}
```

------

## 22.2 Agent 决策校验

流程：

```
Agent Decision

        ↓

DecisionValidator

        ↓

StageManager

        ↓

Execute
```

------

例如：

Agent 返回：

```
{
 "nextStage":"COMPLETED"
}
```

但是当前：

```
INTRODUCTION
```

禁止。

------

# 23. LangGraph Node 设计

------

## 23.1 QuestionGeneratorNode

职责：

生成面试问题。

输入：

```
CandidateProfile

+

InterviewStage

+

Rules

+

History
```

输出：

```
currentQuestion
```

------

调用：

```
InterviewPrompt

↓

AIService

↓

Question
```

------

## 23.2 AnswerAnalyzerNode

职责：

分析用户回答。

输入：

```
Question

+

Answer
```

输出：

```
{
 "correctness":80,

 "depth":70,

 "missing":[]
}
```

------

## 23.3 FollowUpDecisionNode

职责：

决定：

下一步动作。

输出：

```
{
 "action":

 "FOLLOW_UP"
}
```

或者：

```
{
 "action":

 "NEXT_STAGE"
}
```

------

## 23.4 StageTransitionNode

职责：

执行阶段切换。

流程：

```
Decision

↓

StageManager

↓

Update Stage
```

------

## 23.5 EvaluationNode

职责：

生成评分。

输入：

- 面试历史
- CandidateProfile
- JD

输出：

EvaluationResult。

------

## 23.6 SummaryNode

职责：

压缩上下文。

原因：

避免：

Token 无限增长。

------

输入：

```
Message History
```

输出：

```
Summary
```

------

## 23.7 ReportGeneratorNode

职责：

生成最终报告。

输入：

- EvaluationResult
- Interview History
- Profile

输出：

Markdown Report。

------

# 24. Agent 执行流程

完整流程：

```
创建面试


↓

初始化 InterviewState


↓

INTRODUCTION


↓

QuestionGeneratorNode


↓

等待用户回答


↓

AnswerAnalyzerNode


↓

FollowUpDecisionNode


↓

┌──────────────┐
│              │
│ FOLLOW_UP    │
│              │
└──────┬───────┘

       ↓


继续提问


或者


↓

StageTransitionNode


↓

下一阶段


↓

EvaluationNode


↓

SummaryNode


↓

ReportGeneratorNode


↓

COMPLETED
```

------

# 25. Agent Tool 系统

## 25.1 设计原则

Agent 不允许：

直接访问：

- 数据库
- 文件系统
- Service

必须通过 Tool。

------

调用链：

```
Agent Node

↓

Tool Registry

↓

Tool

↓

Application Service

↓

Infrastructure
```

------

## 25.2 Tool 接口

```
public interface AgentTool {


    String name();


    String description();


    ToolResult execute(
        ToolInput input
    );

}
```

------

## 25.3 ToolRegistry

职责：

管理 Agent 可用工具。

```
@Component
public class ToolRegistry {


    Map<String,AgentTool> tools;


}
```

------

## 25.4 V1 Tool 列表

------

## ProfileQueryTool

用途：

查询候选人信息。

------

## KnowledgeSearchTool

用途：

RAG 检索。

------

## HistoryQueryTool

用途：

查询历史回答。

------

## RuleQueryTool

用途：

查询面试规则。

------

# 26. Prompt 管理

## 26.1 设计原则

Prompt：

固定写入代码。

不支持：

- 用户编辑
- 管理后台修改

------

目录：

```
agent/prompt/

├── SystemPrompt.java

├── InterviewPrompt.java

├── AnalysisPrompt.java

├── DecisionPrompt.java

├── EvaluationPrompt.java

└── ReportPrompt.java
```

------

## 26.2 Prompt 版本

增加：

```
PromptVersion.VERSION="v1.0"
```

保存：

```
InterviewSession

↓

prompt_version
```

------

## 26.3 Prompt 职责

| Prompt           | 用途          |
| ---------------- | ------------- |
| SystemPrompt     | 定义Agent身份 |
| InterviewPrompt  | 生成问题      |
| AnalysisPrompt   | 分析回答      |
| DecisionPrompt   | 决策动作      |
| EvaluationPrompt | 评分          |
| ReportPrompt     | 生成报告      |
