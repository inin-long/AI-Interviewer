---
theme: default
title: AI Interviewer 实训验收汇报
info: AI Interviewer 五分钟实训验收汇报
author: 项目组
transition: fade-out
aspectRatio: 16/9
colorSchema: light
lineNumbers: false
---

<div class="cover-grid">
  <section>
    <div class="mini-logo">
      <img src="/assets/app-icon.png" alt="AI Interviewer 图标" />
      <span>AI Interviewer</span>
    </div>
    <h1 class="cover-title">
      实训验收汇报
      <span>让面试训练形成可复盘的能力闭环</span>
    </h1>
    <p class="cover-subtitle">
      一款本地运行、基于简历与知识库的 AI 技术面试训练桌面应用
    </p>
    <div class="cover-meta">
      <span class="brand-chip">JavaFX 桌面端</span>
      <span class="brand-chip">Agent 智能追问</span>
      <span class="brand-chip">RAG 知识增强</span>
    </div>
  </section>

  <section class="cover-visual">
    <ScreenFrame src="/assets/home.png" alt="AI Interviewer 首页" position="center 16%" />
    <div class="floating-proof">
      <b>✓</b>
      <strong>验收主流程已贯通</strong>
      <span>准备 · 面试 · 评估 · 训练</span>
    </div>
  </section>
</div>

<!--
约 25 秒：一句话说明项目定位，直接给出验收结论——主流程已经贯通。本次汇报依次讲背景、业务、功能、技术和分工。
-->

---

<div class="eyebrow">Project background</div>
<h2 class="slide-title">刷题不等于实战，训练必须能够追问和复盘</h2>
<p class="slide-lead">我们不是再做一个题库，而是把个人经历、岗位要求和回答证据组织成持续改进的训练。</p>

<div class="problem-layout">
  <div class="problem-list">
    <div class="problem-item">
      <span class="problem-index">01</span>
      <div><h3>题目不贴合个人</h3><p>固定题库无法围绕简历项目与目标岗位展开。</p></div>
    </div>
    <div class="problem-item">
      <span class="problem-index">02</span>
      <div><h3>追问缺少上下文</h3><p>回答质量、逻辑缺口与前后矛盾难以被连续识别。</p></div>
    </div>
    <div class="problem-item">
      <span class="problem-index">03</span>
      <div><h3>反馈难以落到证据</h3><p>只有笼统分数，无法定位原回答并安排专项训练。</p></div>
    </div>
  </div>

  <div class="answer-statement">
    <small>OUR ANSWER</small>
    <h3>用本地 AI 工作台，完成“一人一岗一方案”的模拟面试。</h3>
    <p>数据保留在本机；智能体根据回答动态追问；报告从逐轮证据生成，并回到下一轮训练。</p>
  </div>
</div>

<!--
约 35 秒：先讲三类痛点，再落到方案。强调产品边界：个人训练工具，不是招聘管理系统，也不是云端 SaaS。
-->

---

<div class="eyebrow">Business flow</div>
<h2 class="slide-title">六个步骤把零散准备变成完整训练闭环</h2>
<p class="slide-lead">每一步都为下一步提供可冻结、可追溯的上下文，保证历史面试能够重现和复盘。</p>

<div class="flow-track">
  <div class="flow-step"><div class="flow-node">01</div><h3>上传简历</h3><p>解析 PDF / DOCX / Markdown / TXT</p></div>
  <div class="flow-step"><div class="flow-node">02</div><h3>确认画像</h3><p>提取技能、经历与项目重点</p></div>
  <div class="flow-step"><div class="flow-node">03</div><h3>配置方案</h3><p>绑定岗位、模式、知识与难度</p></div>
  <div class="flow-step"><div class="flow-node">04</div><h3>动态面试</h3><p>流式提问、追问与阶段转换</p></div>
  <div class="flow-step"><div class="flow-node">05</div><h3>证据评估</h3><p>逐轮沉淀主张、逻辑与能力证据</p></div>
  <div class="flow-step"><div class="flow-node">06</div><h3>复盘训练</h3><p>报告定位原回答并生成专项练习</p></div>
</div>

<div class="flow-outcome">
  <span>输入是个人经历，输出是可行动的能力提升路径</span>
</div>

<!--
约 40 秒：按流程从左到右走一遍。重点强调画像、方案和知识在会话创建时被冻结，报告可以定位原回答，形成闭环。
-->

---

<div class="eyebrow">Functional modules</div>
<h2 class="slide-title">四组功能贯通准备、实战与复盘</h2>

<div class="modules-layout">
  <div class="module-rail">
    <div class="module-row"><span class="mark">01</span><div><h3>训练资产</h3><p>本地账户、简历、候选人画像与数据隔离。</p></div></div>
    <div class="module-row"><span class="mark">02</span><div><h3>方案与知识</h3><p>岗位包、面试参数、题库与私有知识索引。</p></div></div>
    <div class="module-row"><span class="mark">03</span><div><h3>智能面试</h3><p>流式问答、动态追问、压力与场景、断点恢复。</p></div></div>
    <div class="module-row"><span class="mark">04</span><div><h3>评估与训练</h3><p>六维评分、证据定位、重答与专项复试。</p></div></div>
  </div>

  <div class="module-visual">
    <ScreenFrame src="/assets/interview.png" alt="智能面试工作区" position="45% center" />
    <ScreenFrame src="/assets/interview-plan.png" alt="面试方案" position="center 35%" />
    <ScreenFrame src="/assets/interview-analysis.png" alt="评估报告" position="center 24%" />
  </div>
</div>

<!--
约 45 秒：按用户旅程讲四组模块。右侧用真实页面说明，这些模块已经落到桌面端，而不是只停留在架构设计。
-->

---

<div class="eyebrow">Agent workflow</div>
<h2 class="slide-title">智能体先理解回答，再决定下一问</h2>
<p class="slide-lead">LangGraph4j 将一次回答拆成可验证步骤，让追问由证据缺口驱动，而不是只靠一次提示词生成。</p>

<div class="agent-stage">
  <div class="agent-terminal">
    <small>TURN INPUT</small>
    <strong>候选人回答</strong>
    <p>当前阶段、画像快照、知识引用与场景状态共同进入回合。</p>
  </div>

  <div class="agent-core">
    <div class="agent-node"><b>01</b><strong>回答分析</strong><span>理解内容与阶段目标</span></div>
    <div class="agent-node"><b>02</b><strong>主张提取</strong><span>拆成可核验陈述</span></div>
    <div class="agent-node"><b>03</b><strong>逻辑与证据</strong><span>发现缺口并沉淀依据</span></div>
    <div class="agent-node"><b>04</b><strong>一致性检查</strong><span>跨轮澄清潜在冲突</span></div>
    <div class="agent-node"><b>05</b><strong>追问规划</strong><span>选择高价值验证目标</span></div>
    <div class="agent-node"><b>06</b><strong>质量门</strong><span>校验后渲染单个问题</span></div>
  </div>

  <div class="agent-terminal">
    <small>TURN OUTPUT</small>
    <strong>可信下一问</strong>
    <p>问题成功落库后才推进状态；失败可安全降级与恢复。</p>
  </div>
</div>

<div class="agent-foot">
  <div class="agent-proof"><strong>Checkpoint</strong><span>暂停、恢复与版本兼容</span></div>
  <div class="agent-proof"><strong>RAG Tool</strong><span>仅检索本场冻结资料</span></div>
  <div class="agent-proof"><strong>Evidence Ledger</strong><span>评分结论可回到原回答</span></div>
</div>

<!--
约 50 秒：这是项目技术亮点。回答不是直接丢给模型生成下一题，而是经过结构化分析、证据和质量审查。说明 Checkpoint、RAG 范围冻结和证据账本保证可靠性。
-->

---

<div class="eyebrow">Technology choices</div>
<h2 class="slide-title">技术选型服务于三个目标：本地、可恢复、可追溯</h2>

<div class="tech-layout">
  <div class="stack">
    <div class="stack-layer"><b>DESKTOP</b><div><strong>JavaFX + FXML</strong><span>原生桌面交互与统一组件体系</span></div></div>
    <div class="stack-layer"><b>APPLICATION</b><div><strong>Java 21 + Spring Boot</strong><span>单进程生命周期、依赖注入与业务编排</span></div></div>
    <div class="stack-layer"><b>AGENT</b><div><strong>Spring AI + LangGraph4j</strong><span>OpenAI-compatible 接入与状态图工作流</span></div></div>
    <div class="stack-layer"><b>KNOWLEDGE</b><div><strong>Apache Tika + Lucene</strong><span>文档解析、Embedding 与本地向量检索</span></div></div>
    <div class="stack-layer"><b>DATA</b><div><strong>SQLite + MyBatis + Flyway</strong><span>本地持久化、显式 SQL 与版本化迁移</span></div></div>
  </div>

  <div class="choice-list">
    <div class="choice"><i>1</i><div><h3>单进程桌面架构</h3><p>部署简单，不引入 Web 服务；Spring 管业务，JavaFX 管交互。</p></div></div>
    <div class="choice"><i>2</i><div><h3>状态图而非一次性调用</h3><p>面试是长流程，必须能暂停、校验、重试并从 Checkpoint 恢复。</p></div></div>
    <div class="choice"><i>3</i><div><h3>本地数据与检索</h3><p>简历、问答和索引留在本机，仅在 AI 调用时按需联网。</p></div></div>
  </div>
</div>

<!--
约 45 秒：不要逐项念技术名。先讲三个选择标准，再用左侧栈说明每层如何支撑目标。突出单进程、本地数据和状态图。
-->

---

<div class="eyebrow">Team & delivery</div>
<h2 class="slide-title">A 负责产品与前端，B、C 负责后端与智能体</h2>

<div class="team-layout">
  <section class="owner">
    <div class="owner-head">
      <span class="owner-id">A</span>
      <div><h3>A</h3><p>产品方向与前端</p></div>
    </div>
    <ul>
      <li>需求梳理、业务流程与验收口径</li>
      <li>信息架构、交互设计与视觉规范</li>
      <li>JavaFX / FXML 页面、组件与前后端联调</li>
    </ul>
  </section>

  <section class="owner backend">
    <div class="owner-head">
      <span class="owner-id">B+C</span>
      <div><h3>B、C</h3><p>后端与智能体</p></div>
    </div>
    <ul>
      <li>服务层、SQLite 数据模型、任务与文件系统</li>
      <li>简历解析、RAG 索引、AI Provider 接入</li>
      <li>Agent 状态图、证据评估、Checkpoint 与测试</li>
    </ul>
  </section>
</div>

<div class="acceptance-line">
  <strong>验收结论</strong>
  <p>简历 → 画像 → 方案 → 面试 → 评估 → 训练主链路已贯通，并具备本地数据隔离、失败恢复和自动化验证。</p>
  <span>READY</span>
</div>

<!--
约 35 秒：说明 A 与 B/C 的边界，以及接口联调点。最后回扣开场：主流程已贯通，项目具备验收条件。总时长约 4 分 35 秒。
-->
