# AI Interviewer S1 产品设计文档

> 文档定位：S0 MVP 完成后的第一阶段增强版本  
> 版本：S1  
> 产品主题：面试策略与证据引擎  
> 适用范围：本地 AI 技术面试助手  
> 技术基础：Java、JavaFX、Spring Boot、Spring AI、LangGraph4j、SQLite、Lucene

---

# 1. 文档目的

S0 已完成 AI Interviewer 的基础闭环：

```text
用户登录
→ 简历解析
→ 候选人画像
→ 创建面试方案
→ AI 提问与追问
→ 面试暂停和恢复
→ 评分
→ Markdown 报告
```

S1 不以继续增加外围功能为目标，而是重点提升 AI 面试官的“业务判断能力”和“专业面试能力”。

S1 要解决的问题是：

1. AI 提问容易按照固定流程走过场。
2. AI 追问经常停留在话术层面，缺乏真正的验证目标。
3. AI 容易被结构完整但内容空泛的答案误导。
4. AI 评分缺少证据支撑，用户难以理解评分依据。
5. 面试过程缺少跨轮次验证，前后矛盾不容易被识别。
6. AI 缺少真实行业知识，难以像经验丰富的面试官一样判断回答中的漏洞。
7. 情境题和压力题容易演变为随机刁难，缺少结构化设计。
8. 面试完成后缺少复盘、重答和能力提升闭环。

S1 的目标不是让 AI 变得更“凶”，而是让它具备经验丰富面试官的工作方法：

```text
建立判断假设
→ 提取候选人的关键主张
→ 判断主张是否有证据
→ 识别逻辑缺口
→ 选择追问策略
→ 引入约束或场景变量
→ 跨轮次交叉验证
→ 形成可追溯评价
```

---

# 2. S1 产品定位

S1 将产品从：

> 基于 LLM 的结构化模拟面试工具

升级为：

> 基于岗位知识、主张验证、逻辑分析、场景推演和证据积累的智能面试训练系统

S1 的核心产品能力统一命名为：

# Interview Intelligence Engine

中文名称：

> 面试策略与证据引擎

该引擎位于 LangGraph 面试流程和底层模型之间：

```text
Interview Stage
    ↓
Interview Intelligence Engine
    ├── Domain Grounding
    ├── Claim Analysis
    ├── Logic Evaluation
    ├── Probe Planning
    ├── Consistency Checking
    ├── Scenario Direction
    ├── Pressure Control
    └── Evidence Evaluation
    ↓
LLM
```

S1 继续沿用 S0 的以下约束：

- 单 Agent 架构
- 固定面试阶段
- LangGraph4j 状态机
- 本地运行
- 本地多用户
- 在线模型调用
- Spring AI 统一模型接口
- SQLite 结构化数据
- Lucene 本地索引
- JavaFX 单窗口桌面应用
- 不引入多 Agent
- 不引入语音
- 不引入在线代码执行
- 不引入复杂插件市场

---

# 3. S1 核心目标

S1 的核心目标分为五类。

## 3.1 提升追问质量

AI 不再只根据上一句话生成下一句话，而是先判断：

- 候选人提出了哪些重要主张
- 哪些主张尚未被验证
- 哪些主张对岗位最重要
- 哪些回答缺少基线、过程或因果
- 下一轮应该验证什么

## 3.2 提升行业业务能力

AI 面试官必须拥有岗位和行业相关知识，包括：

- 能力模型
- 常用指标
- 常见事故
- 典型决策
- 常见错误
- 追问框架
- 评价标准

## 3.3 提升面试真实性

AI 应能够：

- 深挖细节
- 识别空泛表达
- 引入资源约束
- 动态改变环境
- 通过场景观察候选人的实际决策
- 从不同角度验证同一能力

## 3.4 提升评分可信度

评分必须从“模型主观打分”升级为“证据驱动评价”。

每一个关键分数都必须具备：

- 正向证据
- 负向证据
- 证据来源
- 置信度
- 尚未验证的部分
- 前后矛盾情况

## 3.5 建立训练闭环

面试完成后，用户可以：

- 查看评分证据
- 跳转到对应问答
- 找到逻辑缺口
- 重新回答关键问题
- 对比分支结果
- 生成专项训练计划
- 进行复试验证

---

# 4. S1 功能范围

S1 包含以下功能。

## 4.1 岗位领域知识包

系统提供版本化的岗位知识包 `DomainPack`。

## 4.2 主张提取

系统从候选人的回答中提取关键主张。

## 4.3 逻辑链分析

系统识别回答中的因果链、执行路径、验证方式和逻辑缺口。

## 4.4 追问策略规划

系统根据主张、岗位重要性、证据缺口和剩余时间选择追问策略。

## 4.5 跨轮次一致性检查

系统能够对不同时间点的回答进行交叉验证。

## 4.6 证据链评分

系统逐轮积累能力证据，并在面试结束后汇总评分。

## 4.7 压力等级控制

系统以可控方式引入澄清、质疑、约束和黑天鹅事件。

## 4.8 情境沙盘

系统支持纯文本形式的动态 Case / Simulation。

## 4.9 面试官角色

系统支持不同交互角色，但评分标准保持统一。

## 4.10 单 Agent 质量审查

问题、追问和评分在输出前经过质量校验。

## 4.11 报告增强

报告增加证据、置信度、逻辑链、一致性和压力场景分析。

## 4.12 分支复盘

用户可以从某个 Checkpoint 回到历史问题重新回答。

---

# 5. S1 不包含内容

以下内容不进入 S1：

- 多 Agent 协作
- 数字人面试官
- 视频面试
- 语音识别
- 情绪识别
- 面部表情分析
- 在线代码沙箱
- 自动执行候选人代码
- 图形化复杂架构画布
- MCP 插件市场
- 在线岗位知识包商店
- 企业招聘后台
- 多面试官协作评分
- 真实招聘流程管理
- AI 自动淘汰候选人

这些能力可以在后续 S2 或更远版本讨论。

---

# 6. 领域知识包 DomainPack

## 6.1 设计目标

S0 的知识库主要用于保存候选人的简历、项目资料和学习文档。

S1 新增面试官自己的行业知识：

```text
Candidate Context
候选人简历、项目资料、历史回答

Interviewer Domain Pack
岗位能力模型、行业指标、常见事故、评价标准、追问框架
```

没有 DomainPack，AI 只能成为一个通用追问器，无法成为真正懂岗位的面试官。

## 6.2 DomainPack 内容

每个 DomainPack 包含：

```text
能力模型
岗位指标词典
典型项目场景
常见事故模式
常见错误
技术或业务决策框架
追问策略模板
压力场景模板
评价 Rubric
```

Java 后端示例：

```text
能力模型
├── Java 基础
├── JVM
├── 并发
├── Spring
├── 数据库
├── 缓存
├── 消息队列
├── 分布式系统
├── 系统设计
└── 工程治理

常见事故
├── 缓存击穿
├── 缓存雪崩
├── 消息重复消费
├── 数据库连接池耗尽
├── 热点 Key
├── 慢查询
├── 重试风暴
└── 灰度发布失败
```

产品经理示例：

```text
能力模型
├── 用户洞察
├── 指标分析
├── 需求优先级
├── 实验设计
├── 增长策略
├── 协作推动
└── 商业判断

常见指标
├── DAU
├── 留存率
├── 转化率
├── CAC
├── LTV
├── 漏斗转化
└── 渠道贡献
```

## 6.3 DomainPack 数据结构

```java
public class DomainPack {

    UUID id;

    String roleCode;

    String industryCode;

    String version;

    String displayName;

    List<CompetencyDefinition> competencies;

    List<MetricDefinition> metrics;

    List<FailurePattern> failurePatterns;

    List<ProbePlaybook> probePlaybooks;

    List<ScenarioTemplate> scenarios;

    List<EvaluationRubric> rubrics;
}
```

## 6.4 S1 实现方式

S1 不开发完整的 DomainPack 编辑器。

采用：

```text
内置 JSON / Markdown
→ 启动时解析
→ 建立 Lucene 索引
→ 面试方案选择岗位包
→ 创建面试时生成快照
```

面试开始后必须保存：

- DomainPack ID
- DomainPack Version
- DomainPack Snapshot

后续内置知识包升级不能影响已经完成或正在进行的面试。

---

# 7. 主张提取 Claim Extraction

## 7.1 设计目标

AI 不再把回答视为一个完整文本块，而是将回答拆成需要验证的主张。

例如候选人说：

> 我主导了订单系统重构，通过引入 Redis，使接口响应时间降低了 40%。

系统应提取：

```json
{
  "claims": [
    {
      "type": "OWNERSHIP",
      "content": "候选人主导了订单系统重构"
    },
    {
      "type": "TECHNICAL_DECISION",
      "content": "系统通过引入 Redis 进行优化"
    },
    {
      "type": "METRIC",
      "content": "接口响应时间降低了 40%"
    },
    {
      "type": "CAUSALITY",
      "content": "引入 Redis 导致接口响应时间降低"
    }
  ]
}
```

## 7.2 主张类型

```java
public enum ClaimType {

    FACT,

    METRIC,

    OWNERSHIP,

    CAUSALITY,

    DECISION,

    RESULT,

    CONSTRAINT,

    FAILURE,

    OPINION
}
```

## 7.3 主张状态

```java
public enum ClaimStatus {

    UNVERIFIED,

    PARTIALLY_VERIFIED,

    VERIFIED,

    DISPUTED,

    CLARIFIED,

    REJECTED
}
```

## 7.4 InterviewClaim

```java
public class InterviewClaim {

    UUID claimId;

    UUID sessionId;

    UUID sourceMessageId;

    ClaimType type;

    String content;

    double importance;

    double credibility;

    ClaimStatus status;

    List<String> missingEvidence;

    List<UUID> supportingEvidenceIds;

    List<UUID> conflictingEvidenceIds;

    Instant createdAt;
}
```

## 7.5 主张重要度

主张重要度根据以下因素计算：

```text
岗位相关性
×
能力重要性
×
主张影响范围
×
结果规模
×
当前可信度缺口
```

高重要度主张应优先被验证。

例如：

- “参与过项目”重要度较低
- “主导了核心架构设计”重要度高
- “性能提升 40%”重要度高
- “项目很复杂”重要度低，除非提供具体范围

---

# 8. 逻辑链评估 Logic Chain Evaluation

## 8.1 设计目标

S1 不使用关键词数量判断回答质量。

系统尝试将重要回答拆成以下结构：

```text
背景 / 前提
→ 问题判断
→ 备选方案
→ 选择依据
→ 执行动作
→ 作用机制
→ 最终结果
→ 验证方式
→ 反思与改进
```

## 8.2 逻辑链输出

```json
{
  "premises": [
    "数据库写入成为瓶颈"
  ],
  "problemDiagnosis": "高峰期同步写入导致接口超时",
  "alternatives": [
    "扩容数据库",
    "引入消息队列",
    "批量写入"
  ],
  "decision": "引入消息队列异步削峰",
  "reasoning": "通过排队平滑瞬时流量",
  "actions": [
    "请求写入 Kafka",
    "消费者异步处理订单"
  ],
  "outcome": "峰值期间接口未出现超时",
  "validation": "通过 P99 和队列堆积量验证",
  "gaps": [
    "未说明重复消费处理",
    "未说明消费者积压时的降级策略"
  ]
}
```

## 8.3 逻辑缺口类型

```java
public enum LogicGapType {

    MISSING_BASELINE,

    MISSING_MECHANISM,

    MISSING_EXECUTION_PATH,

    MISSING_ALTERNATIVES,

    MISSING_TRADE_OFF,

    MISSING_VALIDATION,

    MISSING_PERSONAL_CONTRIBUTION,

    MISSING_FAILURE_HANDLING,

    CAUSALITY_JUMP,

    RESULT_WITHOUT_EVIDENCE
}
```

## 8.4 判定原则

系统重点识别：

- 从原因直接跳到结果
- 有结论但没有过程
- 有技术名词但没有执行步骤
- 有结果但没有基线
- 有团队成果但没有个人贡献
- 有方案但没有取舍
- 有成功结果但没有风险处理
- 有数据但没有说明测量方式
- 有归因但没有排除其他变量

---

# 9. 追问策略引擎 Probe Planner

## 9.1 设计目标

AI 不能直接自由生成追问。

系统先产生结构化追问决策，再由语言生成节点将决策转换为自然语言。

## 9.2 追问策略

```java
public enum ProbeStrategy {

    CLARIFY_CONCEPT,

    REQUEST_BASELINE,

    REQUEST_METRIC_BREAKDOWN,

    VERIFY_PERSONAL_OWNERSHIP,

    VERIFY_DATA_SOURCE,

    TRACE_CAUSAL_CHAIN,

    ASK_IMPLEMENTATION_DETAIL,

    ASK_TRADE_OFF,

    ASK_ALTERNATIVE,

    INTRODUCE_CONSTRAINT,

    INTRODUCE_FAILURE,

    CROSS_CHECK_HISTORY,

    CHALLENGE_ASSUMPTION,

    REQUEST_PRIITIZATION
}
```

注意：

`REQUEST_PRIITIZATION` 实现时应修正为：

```java
REQUEST_PRIORITIZATION
```

## 9.3 追问决策输出

```json
{
  "targetClaimId": "claim-102",
  "objective": "验证性能提升的因果关系",
  "strategy": "TRACE_CAUSAL_CHAIN",
  "pressureLevel": "CHALLENGING",
  "reason": "候选人给出了40%的提升结果，但没有提供基线和变量排除方法",
  "expectedEvidence": [
    "优化前后指标",
    "测量方式",
    "同期其他变量"
  ],
  "shouldInjectScenario": false
}
```

## 9.4 追问优先级

追问目标根据以下规则排序：

```text
主张重要性
×
可信度缺口
×
岗位相关性
×
能力覆盖缺口
×
可验证程度
×
剩余面试时间
```

## 9.5 剥洋葱式验证

对“性能提升 40%”可以逐层追问：

```text
第一层：基线是什么？
第二层：数据来自哪里？
第三层：各优化措施贡献多少？
第四层：如何排除其他变量？
第五层：个人负责了什么？
第六层：方案失效时会发生什么？
```

系统不要求每次都完整执行六层。

Probe Planner 根据已经获得的证据动态决定下一层。

---

# 10. 跨轮次一致性检查

## 10.1 设计目标

AI 需要能够从不同阶段、不同问题和不同场景中验证候选人的陈述是否一致。

## 10.2 ClaimLedger

```java
public class ClaimLedger {

    List<InterviewClaim> claims;

    List<ConsistencyIssue> issues;
}
```

## 10.3 矛盾类型

```java
public enum ConsistencyIssueType {

    FACT_CONFLICT,

    TIMELINE_CONFLICT,

    OWNERSHIP_CONFLICT,

    TECHNOLOGY_CONFLICT,

    METRIC_CONFLICT,

    DECISION_PRINCIPLE_CONFLICT,

    VALUE_CONFLICT
}
```

## 10.4 矛盾状态

```java
public enum ConsistencyIssueStatus {

    POTENTIAL,

    CLARIFIED,

    RESOLVED,

    CONFIRMED_CONFLICT
}
```

## 10.5 处理原则

系统不能直接根据语义差异认定候选人撒谎。

正确流程：

```text
发现潜在矛盾
→ 标记 POTENTIAL
→ 生成澄清问题
→ 记录候选人解释
→ 重新判断
→ RESOLVED 或 CONFIRMED_CONFLICT
```

示例：

前面说：

> 我主导了技术方案设计。

后面说：

> 架构选型主要由架构师决定。

AI 应追问：

> 前面你提到自己主导技术方案，刚才又提到架构选型主要由架构师决定。能否具体说明你和架构师分别负责哪些决策？

## 10.6 延迟验证

部分主张不应立即追问。

新增：

```java
public class DeferredProbe {

    UUID id;

    UUID targetClaimId;

    InterviewStage preferredStage;

    ProbeStrategy strategy;

    String reason;

    boolean completed;
}
```

例如：

- 在项目经验阶段记录“拥有完整故障处理经验”
- 在后面的事故场景中验证
- 在压力条件下观察候选人的真实决策

---

# 11. 证据链评分

## 11.1 设计目标

S1 不再由一个模型在面试结束后直接阅读全部对话并给出分数。

系统采用逐轮证据积累：

```text
每轮回答
→ 提取主张
→ 分析逻辑链
→ 收集能力证据
→ 更新置信度
→ 面试结束后汇总
```

## 11.2 EvaluationEvidence

```java
public class EvaluationEvidence {

    UUID id;

    UUID sessionId;

    UUID messageId;

    CompetencyCode competency;

    EvidenceSignal signal;

    double strength;

    double confidence;

    String reason;

    List<UUID> relatedClaimIds;

    Instant createdAt;
}
```

## 11.3 证据信号

```java
public enum EvidenceSignal {

    POSITIVE,

    NEGATIVE,

    NEUTRAL,

    INSUFFICIENT
}
```

## 11.4 证据示例

```json
{
  "competency": "SYSTEM_DESIGN",
  "signal": "POSITIVE",
  "strength": 0.7,
  "confidence": 0.85,
  "messageIds": [
    "m102",
    "m103"
  ],
  "reason": "候选人识别了缓存故障后的数据库回源风险，并提出限流和降级方案"
}
```

## 11.5 最终评分

最终评分考虑：

```text
正向证据
-
负向证据
-
已确认矛盾
+
压力场景表现
+
观点修正能力
+
决策取舍能力
```

S1 仍保留 S0 的固定评分维度：

- 技术能力
- 问题解决能力
- 项目经验
- 系统设计能力
- 沟通能力
- 综合评价

## 11.6 分数与置信度分离

系统必须区分：

```text
能力较弱
```

和：

```text
证据不足
```

示例：

```text
系统设计能力：76
置信度：低
原因：仅获得一组有效证据，且未完成高压场景验证
```

禁止仅用精确分数制造虚假确定性。

## 11.7 证据跳转

报告中的能力分数、优点、缺点和风险点必须关联：

- messageId
- claimId
- evidenceId

用户点击后能够跳转到对应问答。

---

# 12. 压力控制系统

## 12.1 设计目标

压力来自真实约束和决策冲突，不来自冒犯、嘲讽或无意义抬杠。

## 12.2 压力等级

```java
public enum PressureLevel {

    RELAXED,

    STANDARD,

    CHALLENGING,

    HIGH_PRESSURE
}
```

## 12.3 压力阶梯

```text
Level 1：澄清概念
Level 2：要求证据
Level 3：挑战假设
Level 4：引入资源约束
Level 5：注入黑天鹅事件
```

示例：

```text
澄清：
你提到性能明显提升，这里的性能具体指哪个指标？

证据：
优化前后的 P95 和 P99 分别是多少？

挑战：
如何排除机器扩容和流量下降的影响？

约束：
如果不能增加 Redis 集群容量，你会如何处理？

黑天鹅：
Redis 集群不可用，同时数据库 CPU 已经达到 85%，你先做什么？
```

## 12.4 PressureController

负责：

- 当前压力等级
- 连续施压次数
- 是否已经获得足够证据
- 是否需要降低强度
- 是否允许升级压力
- 是否存在无意义重复追问
- 当前用户是否处于正式模拟或教练训练模式

## 12.5 安全规则

系统禁止：

- 侮辱候选人
- 嘲讽候选人
- 人身攻击
- 故意制造不可回答的问题
- 用错误技术结论诱导候选人
- 连续多轮无意义否定
- 将“高压”理解为敌意交流

---

# 13. 情境沙盘 Scenario Engine

## 13.1 设计目标

情境模拟从“听候选人讲故事”升级为“观察候选人如何做决策”。

S1 仅实现纯文本沙盘。

## 13.2 SimulationType

```java
public enum SimulationType {

    INCIDENT_RESPONSE,

    ARCHITECTURE_REVIEW,

    CODE_REVIEW,

    PRODUCT_CASE,

    STAKEHOLDER_CONFLICT,

    RESOURCE_CUT,

    PRIORITY_DECISION
}
```

## 13.3 ScenarioState

```java
public class ScenarioState {

    UUID scenarioId;

    SimulationType type;

    String objective;

    String background;

    Map<String, Object> variables;

    List<ScenarioConstraint> constraints;

    List<ScenarioEvent> events;

    List<CandidateDecision> decisions;

    int currentRound;

    ScenarioStatus status;
}
```

## 13.4 场景结构

每个场景必须包含：

- 候选人的身份
- 当前目标
- 已知事实
- 假设条件
- 隐藏信息
- 初始约束
- 可注入事件
- 评估能力
- 结束条件

## 13.5 技术场景示例

```json
{
  "objective": "设计秒杀订单系统",
  "variables": {
    "peakQps": 20000,
    "inventoryCount": 5000,
    "databaseType": "MySQL",
    "budgetLevel": "MEDIUM"
  },
  "constraints": [
    "不能超卖",
    "允许短时间最终一致",
    "已有 Redis 和 Kafka"
  ]
}
```

后续事件：

```json
{
  "eventType": "RESOURCE_SHOCK",
  "changes": {
    "redisAvailable": false,
    "databaseCpu": 85
  },
  "question": "Redis 集群故障后，你如何降级？"
}
```

## 13.6 场景演进

```text
候选人做出决策
→ Scenario Engine 计算后果
→ 更新变量
→ 注入下一事件
→ AI 要求候选人处理二次后果
```

例如：

```text
候选人选择所有请求回源数据库
→ 数据库连接池耗尽
→ 系统进入二次故障
→ AI 继续追问限流、降级和恢复策略
```

## 13.7 岗位场景类型

技术岗位：

- 系统故障处理
- 架构方案评审
- Code Review
- 技术债务取舍
- 线上事故复盘
- 需求变更下的架构演进

产品岗位：

- 留存突然下降
- 指标冲突
- 需求优先级争议
- 运营与技术冲突
- 预算削减
- 竞品突然发布相似功能

管理岗位：

- 核心成员离职
- 跨团队资源争夺
- 绩效冲突
- 项目延期
- 高层临时改变目标

---

# 14. 面试官角色 Persona

## 14.1 设计原则

角色只决定交流方式，不决定评分标准。

```text
Interaction Persona
决定怎么说

Evaluation Rubric
决定评估什么
```

## 14.2 Persona 类型

```java
public enum InterviewerPersona {

    PROFESSIONAL_INTERVIEWER,

    FUTURE_PEER,

    TECH_LEAD,

    ARCHITECT,

    INCIDENT_COMMANDER,

    PRODUCT_LEADER
}
```

## 14.3 同一决策的不同表达

结构化决策：

> 验证性能提升是否真正来源于 Redis。

专业面试官：

> 你如何证明这 40% 的改善主要来自 Redis，而不是流量下降或机器扩容？

未来同事：

> 我们后续可能会复用这套方案，我比较关心收益归因。你们当时是怎么确认主要收益确实来自 Redis 的？

技术负责人：

> 如果让我批准这次改造，我需要看到可靠的收益归因。你会用哪些数据证明这个结论？

## 14.4 协作型能力观察

系统需要记录：

- 是否主动澄清问题
- 是否承认信息不足
- 是否能够修正观点
- 是否愿意吸收反对意见
- 是否只是在迎合
- 是否能坚持有依据的不同意见
- 是否能够与他人共同推进问题

---

# 15. 面试模式

## 15.1 正式模拟模式

特点：

- 不展示即时评分
- 不展示回答缺失项
- 不主动提供提示
- 不允许重新回答当前问题
- 面试结束后统一生成报告

## 15.2 教练训练模式

特点：

- 回答后显示遗漏点
- 可以请求提示
- 可以重新组织回答
- 可以查看参考结构
- 可以针对弱点重复训练
- 教练提示不进入正式评分证据

## 15.3 情境沙盘模式

特点：

- 围绕一个持续变化的场景展开
- 重点评估决策、取舍、风险意识和协作
- 使用 Scenario Engine 管理状态
- 支持多轮事件注入

## 15.4 模式与评分隔离

正式模拟、教练训练和情境沙盘必须使用不同的规则快照。

教练模式中的提示内容不能被用作候选人能力证据。

---

# 16. 问题质量审查

## 16.1 设计目标

问题生成后不能直接展示。

增加：

```text
Question Planner
→ Question Renderer
→ Question Quality Gate
→ 通过后展示
```

## 16.2 审查内容

检查：

- 是否符合当前 Stage
- 是否符合岗位能力模型
- 是否围绕明确验证目标
- 是否与已问问题重复
- 是否一次提出过多问题
- 是否泄露参考答案
- 是否捏造候选人信息
- 是否使用未经声明的背景
- 难度是否匹配
- 是否存在无意义压力
- 是否能够获得可评估证据

## 16.3 不通过处理

```text
问题不通过
→ 返回失败原因
→ 重新生成一次
→ 仍不通过则降级为规则模板问题
```

避免无限重试。

---

# 17. 防幻觉与事实边界

## 17.1 信息类型

系统必须区分：

```text
已知事实
来源于简历、JD、知识库或候选人回答

领域知识
来源于 DomainPack

假设条件
由 Scenario Engine 明确声明为假设
```

## 17.2 禁止行为

AI 禁止：

- 捏造公司业务背景
- 捏造候选人项目数据
- 捏造行业现状
- 将假设描述为事实
- 将通用经验描述为候选人的真实经历
- 用错误技术结论故意误导候选人

## 17.3 正确表达

错误：

> 在行业大盘整体下滑的背景下，你们仍增长了 20%。

正确：

> 假设同期行业整体流量下降了 15%，你会如何重新验证这次增长的真实贡献？

---

# 18. LangGraph 流程改造

## 18.1 S0 流程

```text
QuestionGenerator
→ 等待回答
→ AnswerAnalyzer
→ FollowUpDecision
→ StageTransition
```

## 18.2 S1 流程

```text
LoadContext
    ↓
QuestionOrScenarioPlanner
    ↓
QuestionRenderer
    ↓
QuestionQualityGate
    ↓
等待用户回答
    ↓
ClaimExtractor
    ↓
LogicChainEvaluator
    ↓
EvidenceCollector
    ↓
ConsistencyCheck
    ↓
CoverageUpdater
    ↓
ProbePlanner
    ↓
PressureController
    ↓
ScenarioDirector
    ↓
DecisionValidator
    ↓
继续追问 / 注入场景 / 切换阶段 / 结束
```

## 18.3 节点职责

### LoadContextNode

加载：

- 候选人画像
- 岗位描述
- DomainPack
- 历史主张
- 当前能力覆盖
- 场景状态
- 延迟验证任务

### QuestionOrScenarioPlannerNode

决定下一步是：

- 普通问题
- 追问
- 交叉验证
- 场景事件
- 阶段切换

### QuestionRendererNode

根据：

- Persona
- 压力等级
- 追问目标
- 面试模式

生成自然语言问题。

### QuestionQualityGateNode

校验问题质量。

### ClaimExtractorNode

从候选人回答中提取主张。

### LogicChainEvaluatorNode

分析逻辑链结构和缺口。

### EvidenceCollectorNode

生成能力证据。

### ConsistencyCheckNode

与历史主张交叉比对。

### CoverageUpdaterNode

更新各能力维度：

- 覆盖程度
- 证据数量
- 当前置信度
- 是否仍需验证

### ProbePlannerNode

选择下一轮追问目标和策略。

### PressureControllerNode

决定压力等级是否调整。

### ScenarioDirectorNode

维护场景变量和事件。

### DecisionValidatorNode

校验：

- 是否允许进入下一 Stage
- 是否允许结束
- 是否违反系统规则
- 是否超过连续施压限制
- 是否存在没有验证目标的追问

---

# 19. InterviewState 扩展

```java
public class InterviewState {

    UUID sessionId;

    UUID userId;

    InterviewStage stage;

    InterviewMode mode;

    InterviewerPersona persona;

    PressureLevel pressureLevel;

    CandidateProfile profile;

    JobContext jobContext;

    DomainPackSnapshot domainPack;

    List<Message> messages;

    ClaimLedger claimLedger;

    EvidenceLedger evidenceLedger;

    List<DeferredProbe> deferredProbes;

    ScenarioState activeScenario;

    InterviewCoverage coverage;

    InterviewStrategy strategy;

    String contextSummary;
}
```

## 19.1 InterviewCoverage

```json
{
  "competencies": {
    "SYSTEM_DESIGN": {
      "importance": 0.9,
      "evidenceCount": 4,
      "confidence": 0.78,
      "coverage": 0.65
    }
  }
}
```

下一轮问题必须综合考虑：

```text
岗位重要能力
尚未覆盖能力
低置信度结论
待验证主张
待澄清矛盾
剩余时间
当前场景状态
```

---

# 20. 数据库扩展

S1 新增以下表。

## 20.1 domain_pack

```sql
CREATE TABLE domain_pack (
    id TEXT PRIMARY KEY,
    role_code VARCHAR(64) NOT NULL,
    industry_code VARCHAR(64),
    display_name VARCHAR(128) NOT NULL,
    version VARCHAR(32) NOT NULL,
    content_json TEXT NOT NULL,
    enabled INTEGER NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL
);
```

## 20.2 interview_claim

```sql
CREATE TABLE interview_claim (
    id TEXT PRIMARY KEY,
    session_id TEXT NOT NULL,
    source_message_id TEXT NOT NULL,
    claim_type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    importance REAL NOT NULL,
    credibility REAL NOT NULL,
    status VARCHAR(32) NOT NULL,
    missing_evidence_json TEXT,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL
);
```

## 20.3 evaluation_evidence

```sql
CREATE TABLE evaluation_evidence (
    id TEXT PRIMARY KEY,
    session_id TEXT NOT NULL,
    message_id TEXT NOT NULL,
    competency_code VARCHAR(64) NOT NULL,
    signal VARCHAR(32) NOT NULL,
    strength REAL NOT NULL,
    confidence REAL NOT NULL,
    reason TEXT NOT NULL,
    related_claim_ids_json TEXT,
    create_time DATETIME NOT NULL
);
```

## 20.4 consistency_issue

```sql
CREATE TABLE consistency_issue (
    id TEXT PRIMARY KEY,
    session_id TEXT NOT NULL,
    issue_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    description TEXT NOT NULL,
    related_claim_ids_json TEXT NOT NULL,
    clarification_message_id TEXT,
    resolution TEXT,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL
);
```

## 20.5 deferred_probe

```sql
CREATE TABLE deferred_probe (
    id TEXT PRIMARY KEY,
    session_id TEXT NOT NULL,
    target_claim_id TEXT NOT NULL,
    preferred_stage VARCHAR(64),
    strategy VARCHAR(64) NOT NULL,
    reason TEXT NOT NULL,
    completed INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL
);
```

## 20.6 scenario_session

```sql
CREATE TABLE scenario_session (
    id TEXT PRIMARY KEY,
    interview_session_id TEXT NOT NULL,
    scenario_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    state_json TEXT NOT NULL,
    current_round INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL
);
```

## 20.7 session_branch

```sql
CREATE TABLE session_branch (
    id TEXT PRIMARY KEY,
    source_session_id TEXT NOT NULL,
    source_checkpoint_id TEXT NOT NULL,
    parent_branch_id TEXT,
    title VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL
);
```

---

# 21. 面试方案页面改造

面试方案新增配置。

## 21.1 面试模式

```text
正式模拟
教练训练
情境沙盘
```

## 21.2 面试官角色

```text
专业面试官
未来同事
技术负责人
架构师
事故指挥者
产品负责人
```

## 21.3 压力等级

```text
轻松
标准
挑战
高压
```

## 21.4 验证严格度

```text
标准
严格
```

## 21.5 场景比例

配置本次面试中 Simulation 所占比例。

示例：

```text
0%
20%
30%
50%
```

## 21.6 DomainPack

允许用户选择：

- 岗位类型
- 行业类型
- 知识包版本

S1 默认提供少量内置知识包，不做用户自定义编辑。

---

# 22. 面试页面改造

## 22.1 普通面试布局

继续使用 S0 的聊天主界面。

新增顶部状态：

- 当前阶段
- 当前模式
- 当前 Persona
- 当前压力等级
- 剩余时间
- 当前场景状态

正式模拟模式不显示：

- 实时评分
- 主张状态
- 逻辑缺口
- 能力置信度

## 22.2 情境沙盘布局

```text
┌────────────────────────────────────────┐
│ 场景目标 / 当前时间 / 当前约束         │
├───────────────────────┬────────────────┤
│                       │ 已知信息       │
│ 对话区域              │ 最新事件       │
│                       │ 已作决策       │
├───────────────────────┴────────────────┤
│ 候选人输入                              │
└────────────────────────────────────────┘
```

## 22.3 教练模式

回答后可以展示：

```text
已经覆盖
缺失内容
逻辑缺口
建议回答结构
是否重新回答
是否请求提示
```

这些内容不得进入正式能力证据。

---

# 23. 报告改造

S1 延续固定 Markdown 模板，但扩展章节。

```markdown
# 技术面试报告

## 1. 面试基本信息

## 2. 综合结论

## 3. 能力评分与置信度

## 4. 关键能力证据

## 5. 核心主张可信度

## 6. 逻辑链完整度

## 7. 压力场景表现

## 8. 决策与取舍风格

## 9. 协作与观点修正能力

## 10. 前后不一致及澄清结果

## 11. 优势

## 12. 风险点

## 13. 改进建议

## 14. 学习计划

## 15. 关键问答证据
```

## 23.1 报告表述原则

禁止直接写：

> 候选人疑似夸大。

推荐写法：

> 关于“性能提升 40%”的结论，候选人未能提供明确基线、测量周期和变量排除方法，因此该主张当前证据强度较低。

报告应保持：

- 客观
- 基于证据
- 可跳转
- 不做人格判断
- 不将信息不足直接等同于能力不足

---

# 24. 时间旅行与分支复盘

## 24.1 功能目标

用户可以从某个历史 Checkpoint 创建分支，重新回答关键问题。

```text
原始回答 A
→ 原始追问
→ 原始报告

重新回答 B
→ 新追问分支
→ 分支评价
→ 对比结果
```

## 24.2 使用流程

```text
报告中找到薄弱问题
→ 点击“重新回答”
→ 从该问题前的 Checkpoint 创建 Branch
→ 重新回答
→ 继续若干轮
→ 生成分支评价
→ 对比原始回答与新回答
```

## 24.3 分支对比内容

- 逻辑链完整度
- 证据数量
- 评分变化
- 追问变化
- 观点修正
- 是否解决原始缺口

S1 不要求分支生成完整独立面试报告，可以先生成“局部对比报告”。

---

# 25. 学习与复试闭环

S1 的报告不再是流程终点。

```text
面试
→ 弱点识别
→ 学习建议
→ 专项训练
→ 分支重答
→ 复试验证
→ 能力变化
```

S1 可以先实现：

- 根据负向证据生成学习主题
- 根据逻辑缺口生成专项练习
- 从知识库关联相关文档
- 创建专项面试方案
- 记录复试前后评分变化

能力趋势属于 S1 后半阶段，不作为第一批必须交付功能。

---

# 26. 错误降级与稳定性

S1 节点明显增加，必须避免工作流过度脆弱。

## 26.1 节点失败降级

```text
ClaimExtractor 失败
→ 保存原始回答
→ 使用基础 AnswerAnalyzer

LogicChainEvaluator 失败
→ 跳过逻辑链评价
→ 不阻断下一问题

ConsistencyCheck 失败
→ 延迟到后续轮次重试

QuestionQualityGate 连续失败
→ 使用 DomainPack 中的模板问题

ScenarioDirector 失败
→ 结束当前场景
→ 返回普通面试流程
```

## 26.2 结构化输出校验

所有决策节点必须：

- 使用 JSON Schema
- 校验枚举值
- 校验必填字段
- 校验数值范围
- 最多自动修复一次
- 修复失败后降级

## 26.3 流式输出

只允许 `QuestionRendererNode` 和报告展示阶段流式输出。

内部分析节点采用非流式结构化响应，避免状态不完整。

---

# 27. 性能和 Token 控制

S1 会增加多个 LLM 节点，必须控制成本。

## 27.1 节点合并原则

首期可合并：

```text
ClaimExtractor
+
LogicChainEvaluator
```

在一次结构化调用中完成。

可合并：

```text
ConsistencyCheck
+
EvidenceCollector
```

但必须保持输出字段独立。

## 27.2 运行频率

- ClaimExtractor：每轮执行
- LogicChainEvaluator：重要回答执行
- ConsistencyCheck：每 3 至 5 轮或命中关联主题时执行
- QuestionQualityGate：每个问题执行
- EvidenceCollector：每轮执行
- ScenarioDirector：仅场景模式执行
- SummaryNode：达到 Token 阈值执行

## 27.3 上下文输入

内部节点不读取完整聊天历史。

优先输入：

- 最近若干轮消息
- Context Summary
- ClaimLedger 摘要
- EvidenceLedger 摘要
- 当前 Stage
- 当前验证目标
- 相关历史主张

---

# 28. S1 实施阶段

## Phase S1-1：领域知识与主张验证

实现：

- DomainPack
- DomainPack Loader
- ClaimExtractorNode
- InterviewClaim
- ClaimLedger
- ProbePlannerNode
- QuestionRendererNode

验收：

- AI 能识别回答中的关键主张
- AI 追问围绕具体主张
- AI 不再只生成通用追问

## Phase S1-2：逻辑链与证据评分

实现：

- LogicChainEvaluatorNode
- LogicGap
- EvidenceCollectorNode
- EvaluationEvidence
- EvidenceLedger
- 评分置信度

验收：

- 每轮回答能产生能力证据
- 报告能展示证据来源
- 能区分能力弱和证据不足

## Phase S1-3：跨轮一致性验证

实现：

- ConsistencyCheckNode
- ConsistencyIssue
- DeferredProbe
- 延迟验证调度

验收：

- 系统能识别前后潜在矛盾
- 系统通过澄清问题解决矛盾
- 不直接做负面人格判断

## Phase S1-4：压力控制与动态场景

实现：

- PressureController
- ScenarioEngine
- ScenarioDirectorNode
- 内置场景模板

验收：

- AI 能引入资源变化和故障事件
- 场景前后变量保持一致
- 压力追问不变成随机刁难

## Phase S1-5：Persona 与质量审查

实现：

- InterviewerPersona
- QuestionQualityGateNode
- Persona Renderer
- 协作型证据

验收：

- 相同追问目标可输出不同语气
- 角色不影响评分标准
- 不合格问题被拦截

## Phase S1-6：报告、分支复盘和训练闭环

实现：

- 新报告模板
- Evidence 跳转
- Session Branch
- Branch Comparison
- 专项训练建议

验收：

- 报告中的判断均可追溯
- 用户能重新回答关键问题
- 系统能对比两次回答差异

---

# 29. S1 优先级

|优先级|能力|原因|
|---|---|---|
|S|DomainPack|决定 AI 是否真正懂岗位|
|S|Claim Ledger|让系统知道候选人具体说了什么|
|S|Probe Planner|让追问具有明确验证目标|
|S|Evidence Ledger|让评分可解释、可追溯|
|A|Logic Chain Evaluator|摆脱关键词匹配|
|A|Consistency Check|提升跨轮验证能力|
|A|Question Quality Gate|减少重复、幻觉和无效问题|
|A|Pressure Controller|实现可控的犀利追问|
|A|Scenario Engine|提升真实决策模拟|
|B|Persona|提升交互真实性|
|B|Branch Replay|提升训练价值|
|B|学习与复试闭环|提升长期留存|
|C|代码仓库面试|创新性高，但应放在后续阶段|
|C|Agent 调试台|适合开发和答辩|
|C|MCP 工具体系|暂时只保留架构扩展点|

---

# 30. S1 验收标准

S1 完成后，系统至少需要满足以下标准。

## 30.1 追问能力

- 每个追问都有明确目标
- 追问能够关联某个主张或能力缺口
- 不出现连续重复的通用追问
- 能对数据、因果、个人贡献和取舍进行深挖

## 30.2 行业能力

- 面试方案必须绑定 DomainPack
- 面试问题能够引用岗位能力模型
- 情境题符合目标岗位真实问题
- AI 不依赖通用面试话术完成全部流程

## 30.3 逻辑评估

- 能识别回答中的逻辑缺口
- 能区分结论、执行过程和验证方式
- 能识别有结果无基线的回答
- 能识别团队成果和个人贡献的混淆

## 30.4 一致性

- 能对历史主张进行检索和比较
- 潜在矛盾必须通过澄清确认
- 报告中能展示已解决和未解决的一致性问题

## 30.5 场景能力

- 场景状态必须结构化保存
- 变量变化不能前后矛盾
- 候选人决策必须影响后续事件
- 高压模式不能出现攻击性表达

## 30.6 评分能力

- 每个核心评分至少关联一条证据
- 报告显示评分置信度
- 证据不足不能直接等同能力不足
- 用户可以从报告跳转到对应问答

## 30.7 稳定性

- 单个分析节点失败不能导致整场面试失败
- 结构化输出失败有降级策略
- Checkpoint 能保存新增状态
- 老版本 Checkpoint 有版本迁移或拒绝恢复提示

---

# 31. 最终产品形态

S0 的产品核心是：

```text
AI 能够完成一场结构化面试
```

S1 的产品核心升级为：

```text
AI 能够像经验丰富的面试官一样：

知道候选人说了什么
知道哪里值得怀疑
知道下一步应验证什么
知道如何引入真实约束
知道如何比较前后回答
知道评分依据来自哪里
知道哪些结论仍然缺少证据
```

S1 的差异化不应表现为：

- 语气更强硬
- 问题更难
- 追问更多
- Prompt 更长

而应表现为：

```text
更懂岗位
更懂证据
更懂逻辑
更懂决策
更懂验证
更懂复盘
```

这将使 AI Interviewer 从普通的 AI 聊天面试工具，升级为具有面试策略、行业判断和证据评估能力的智能训练系统。
