---
theme: default
title: AI 模拟面试官项目验收报告
info: AI 模拟面试官项目验收汇报
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
      AI 模拟面试官
      <span>项目验收报告</span>
    </h1>
    <p class="cover-subtitle">
      智能面试与职业发展一体化平台
    </p>
    <div class="cover-meta">
      <span class="brand-chip">拟真 AI 面试</span>
      <span class="brand-chip">本地 RAG</span>
      <span class="brand-chip">成长闭环</span>
    </div>
    <p class="cover-date">汇报日期：2026.07</p>
  </section>

  <section class="cover-visual">
    <ScreenFrame src="/assets/home.png" alt="AI 模拟面试官首页" position="center 16%" />
    <div class="floating-proof">
      <b>✓</b>
      <strong>项目主流程已贯通</strong>
      <span>准备 · 面试 · 测评 · 规划</span>
    </div>
  </section>
</div>

---

<div class="eyebrow">Contents</div>
<h2 class="slide-title">目录</h2>
<p class="slide-lead">从建设目标与验收标准出发，依次说明系统能力、质量结果与创新价值。</p>

<div class="contents-grid">
  <div class="contents-item"><b>01</b><span>项目背景与建设目标</span></div>
  <div class="contents-item"><b>02</b><span>需求分析与验收标准</span></div>
  <div class="contents-item"><b>03</b><span>系统技术架构</span></div>
  <div class="contents-item"><b>04</b><span>功能模块一览</span></div>
  <div class="contents-item"><b>05</b><span>核心能力 · AI 面试引擎</span></div>
  <div class="contents-item"><b>06</b><span>核心能力 · 关键机制</span></div>
  <div class="contents-item"><b>07</b><span>知识库（RAG）模块</span></div>
  <div class="contents-item"><b>08</b><span>简历 / 测评 / 规划</span></div>
  <div class="contents-item"><b>09</b><span>面试方案 / 记录 / 技巧库</span></div>
  <div class="contents-item"><b>10</b><span>题库 / 任务 / 设置 / 认证</span></div>
  <div class="contents-item"><b>11</b><span>系统测试与质量保障</span></div>
  <div class="contents-item"><b>12</b><span>需求完成情况对照</span></div>
  <div class="contents-item"><b>13</b><span>项目创新点</span></div>
  <div class="contents-item"><b>14</b><span>结论</span></div>
</div>

---

<div class="eyebrow">Background · Objectives</div>
<h2 class="slide-title">把分散的求职工具，组织成可持续优化的成长闭环</h2>

<div class="two-panel objective-layout">
  <section>
    <div class="section-label">建设背景</div>
    <div class="problem-list">
      <div class="problem-item">
        <span class="problem-index">01</span>
        <div><h3>缺少真实场景</h3><p>求职者普遍缺乏真实面试环境与即时反馈。</p></div>
      </div>
      <div class="problem-item">
        <span class="problem-index">02</span>
        <div><h3>练习成本较高</h3><p>预约真人模拟成本高，也难以按需反复练习。</p></div>
      </div>
      <div class="problem-item">
        <span class="problem-index">03</span>
        <div><h3>工具彼此割裂</h3><p>简历、测评与规划工具分散，数据无法联动。</p></div>
      </div>
    </div>
  </section>

  <section class="objective-card">
    <small>建设目标</small>
    <h3>构建一个本地运行的智能面试与职业发展应用</h3>
    <ul class="check-list">
      <li>拟真 AI 面试：追问、点评与评分</li>
      <li>简历解析与候选人能力画像</li>
      <li>RAG 岗位知识增强</li>
      <li>霍兰德测评与职业规划</li>
      <li>训练 → 测评 → 规划 → 优化</li>
    </ul>
  </section>
</div>

<div class="metric-strip six">
  <div><strong>12+</strong><span>功能模块可运行</span></div>
  <div><strong>双轨</strong><span>技术深挖 + 动机穿插</span></div>
  <div><strong>RAG</strong><span>本地向量检索增强</span></div>
  <div><strong>联动</strong><span>简历 / 画像 / 测评 / 规划</span></div>
  <div><strong>五维</strong><span>评分与复盘报告</span></div>
  <div><strong>Local</strong><span>数据本地存储</span></div>
</div>

---

<div class="eyebrow">Requirements · 验收依据</div>
<h2 class="slide-title">12 项功能需求，覆盖从身份认证到持续训练</h2>

<table class="compact-table requirements-table">
  <thead><tr><th>编号</th><th>功能需求描述</th><th>对应模块</th><th>编号</th><th>功能需求描述</th><th>对应模块</th></tr></thead>
  <tbody>
    <tr><td>FR-1</td><td>用户注册 / 登录与身份鉴权</td><td>认证</td><td>FR-7</td><td>职业规划路径生成</td><td>职业规划</td></tr>
    <tr><td>FR-2</td><td>开场、深挖追问、动机题、收尾评分</td><td>面试引擎</td><td>FR-8</td><td>面试方案制定与历史记录复盘</td><td>方案 / 记录</td></tr>
    <tr><td>FR-3</td><td>文档上传、切片、向量化与检索增强</td><td>RAG</td><td>FR-9</td><td>岗位题库管理与筛选练习</td><td>题库</td></tr>
    <tr><td>FR-4</td><td>简历导入、解析与智能优化建议</td><td>简历管理</td><td>FR-10</td><td>面试技巧库检索学习</td><td>技巧库</td></tr>
    <tr><td>FR-5</td><td>候选人能力画像与岗位匹配度</td><td>画像</td><td>FR-11</td><td>后台任务监控管理</td><td>任务中心</td></tr>
    <tr><td>FR-6</td><td>霍兰德六型职业测评</td><td>职业测评</td><td>FR-12</td><td>模型、主题与数据设置</td><td>设置</td></tr>
  </tbody>
</table>

<div class="nfr-line">
  <strong>非功能需求</strong>
  <span>本地数据与隐私保护</span>
  <span>离线可用</span>
  <span>流式低延迟</span>
  <span>界面友好</span>
  <span>Flyway 迁移与分层架构</span>
</div>

---

<div class="eyebrow">Architecture</div>
<h2 class="slide-title">五层架构把桌面交互、智能能力与本地数据解耦</h2>

<div class="architecture-stack">
  <div class="architecture-layer presentation">
    <b>表现层</b><strong>Presentation</strong>
    <span>JavaFX 原生桌面 UI · FXML 声明式布局 · CSS 主题样式</span>
  </div>
  <div class="architecture-layer business">
    <b>业务层</b><strong>Business Logic</strong>
    <span>Spring Boot IoC / AOP · MyBatis ORM · LangGraph4j 多智能体状态机</span>
  </div>
  <div class="architecture-layer capability">
    <b>能力层</b><strong>Capabilities</strong>
    <span>RAG 向量检索 · 简历解析优化 · RIASEC 测评 · 评分报告 · 后台任务</span>
  </div>
  <div class="architecture-layer storage">
    <b>存储层</b><strong>Storage</strong>
    <span>SQLite 业务库 · 本地向量库 · Flyway 版本迁移 · 文件系统</span>
  </div>
  <div class="architecture-layer model">
    <b>模型层</b><strong>Model</strong>
    <span>大语言模型 API · 流式对话 · Function Calling · 可配置切换</span>
  </div>
</div>

<div class="architecture-note">
  <strong>设计原则</strong>
  <span>本地优先</span><span>状态可恢复</span><span>能力可替换</span><span>数据可追溯</span>
</div>

---

<div class="eyebrow">Modules</div>
<h2 class="slide-title">14 个模块覆盖准备、实战、复盘与职业发展</h2>

<div class="module-grid">
  <div class="module-card"><b>01</b><div><h3>首页仪表盘</h3><p>全局概览 / 快捷入口</p></div></div>
  <div class="module-card"><b>02</b><div><h3>面试方案</h3><p>定制面试计划</p></div></div>
  <div class="module-card"><b>03</b><div><h3>面试记录</h3><p>留痕 / 复盘报告</p></div></div>
  <div class="module-card"><b>04</b><div><h3>简历管理</h3><p>导入 / 智能优化</p></div></div>
  <div class="module-card"><b>05</b><div><h3>候选人画像</h3><p>技能雷达 / 匹配度</p></div></div>
  <div class="module-card"><b>06</b><div><h3>知识库（RAG）</h3><p>文档向量检索</p></div></div>
  <div class="module-card"><b>07</b><div><h3>岗位题库</h3><p>真题筛选练习</p></div></div>
  <div class="module-card"><b>08</b><div><h3>职业测评</h3><p>霍兰德六型</p></div></div>
  <div class="module-card"><b>09</b><div><h3>面试技巧库</h3><p>考点 / 答题框架</p></div></div>
  <div class="module-card"><b>10</b><div><h3>职业规划</h3><p>路径 / 行动清单</p></div></div>
  <div class="module-card"><b>11</b><div><h3>任务中心</h3><p>后台任务监控</p></div></div>
  <div class="module-card"><b>12</b><div><h3>系统设置</h3><p>模型 / 主题配置</p></div></div>
  <div class="module-card"><b>13</b><div><h3>用户认证</h3><p>注册 / 登录 / 鉴权</p></div></div>
  <div class="module-card primary"><b>14</b><div><h3>AI 面试引擎</h3><p>追问 / 评分核心</p></div></div>
</div>

<div class="flow-outcome"><span>覆盖从面试准备到结果复盘的完整用户旅程</span></div>

---

<div class="eyebrow">Core · 多智能体编排</div>
<h2 class="slide-title">13 节点 InterviewGraph，让面试流程可控、可回溯</h2>
<p class="slide-lead">LangGraph4j 将开场、探查、决策、质检、分析和收尾组织为清晰的状态流转。</p>

<div class="engine-layout">
  <section class="engine-copy">
    <div class="numbered-point"><b>01</b><div><h3>多节点编排</h3><p>包含 Opening、ProbePlanner、FollowUpDecision、QuestionQualityGate、DecisionValidator、ScenarioDirector、AnswerAnalyzer、Closing 等节点。</p></div></div>
    <div class="numbered-point"><b>02</b><div><h3>状态流转</h3><p>OPENING → PROBE_PLAN → FOLLOW_UP_DECISION → QUESTION_GATE → DECISION_VALIDATE → ANSWER_ANALYZER → CLOSING。</p></div></div>
    <div class="numbered-point"><b>03</b><div><h3>拟人化体验</h3><p>模拟真实面试中的追问、承接、转折与收尾，而非简单的一问一答机器人。</p></div></div>
  </section>

  <section class="engine-graph">
    <div class="graph-node start">Opening</div>
    <div class="graph-row">
      <div class="graph-node">Probe<br>Planner</div>
      <div class="graph-node">Follow Up<br>Decision</div>
      <div class="graph-node">Question<br>Gate</div>
    </div>
    <div class="graph-row">
      <div class="graph-node">Decision<br>Validator</div>
      <div class="graph-node accent">Answer<br>Analyzer</div>
      <div class="graph-node">Scenario<br>Director</div>
    </div>
    <div class="graph-node end">Closing</div>
  </section>
</div>

---

<div class="eyebrow">Core · 细节实现</div>
<h2 class="slide-title">四项关键机制共同保证面试自然、稳定且可评估</h2>

<div class="mechanism-grid">
  <section class="mechanism-card blue">
    <small>01</small><h3>自适应追问</h3>
    <ul>
      <li>ProbePlan 规划追问方向</li>
      <li>AnswerAnalyzer 分析回答深度与证据</li>
      <li>FollowUpDecision 决定深挖或转场</li>
      <li>质量门与决策校验过滤空泛、重复问题</li>
    </ul>
  </section>
  <section class="mechanism-card orange">
    <small>02</small><h3>动机题穿插</h3>
    <ul>
      <li>覆盖选岗动机、职业规划、优劣势与行业兴趣</li>
      <li>motivationTopic 字段标记动机题</li>
      <li>质量门控提供针对性校验</li>
    </ul>
  </section>
  <section class="mechanism-card green">
    <small>03</small><h3>实时流式对话</h3>
    <ul>
      <li>LLM 流式 SSE 逐字输出</li>
      <li>TRIVIAL_ANSWER_PATTERNS 快速通道</li>
      <li>DEFAULT_TIMEOUT 超时保护与优雅降级</li>
      <li>长回答流畅显示，界面不卡死</li>
    </ul>
  </section>
  <section class="mechanism-card purple">
    <small>04</small><h3>五维评分体系</h3>
    <div class="score-dimensions">
      <span>TechnicalDepth</span><span>Communication</span><span>ProblemSolving</span>
      <span>DomainKnowledge</span><span>LearningPotential</span>
    </div>
    <div class="score-rule"><strong>≥ 70</strong><span>提示复试资格</span></div>
  </section>
</div>

---

<div class="eyebrow">Knowledge · 让面试官懂岗位</div>
<h2 class="slide-title">本地 RAG 将岗位知识转化为更有针对性的追问</h2>
<p class="slide-lead">文档从接入到检索全程在本地管理，只把当前问题需要的片段注入面试上下文。</p>

<div class="rag-flow">
  <div class="rag-step"><b>01</b><h3>文档接入</h3><p>上传岗位 JD、技术文档等常见文本类型。</p></div>
  <div class="rag-arrow">→</div>
  <div class="rag-step"><b>02</b><h3>切片与向量化</h3><p>自动 chunking 与 embedding，写入本地向量库。</p></div>
  <div class="rag-arrow">→</div>
  <div class="rag-step"><b>03</b><h3>检索增强</h3><p>按话题检索 Top-K 片段，并注入 Prompt。</p></div>
  <div class="rag-arrow">→</div>
  <div class="rag-step"><b>04</b><h3>岗位化追问</h3><p>问题更贴近目标岗位、业务与技术语境。</p></div>
</div>

<div class="rag-bottom">
  <div class="rag-proof">
    <small>生命周期</small>
    <strong>DomainPackService</strong>
    <span>统一管理知识包、索引与检索范围</span>
  </div>
  <div class="rag-proof">
    <small>隐私边界</small>
    <strong>Knowledge stays local</strong>
    <span>知识数据仅存本地，不上传第三方</span>
  </div>
</div>

---

<div class="eyebrow">Profile · Assessment · Plan</div>
<h2 class="slide-title">简历、画像、测评和规划共享同一份成长数据</h2>

<div class="profile-layout">
  <section class="profile-track">
    <div class="profile-step"><b>01</b><div><h3>简历管理</h3><p>ResumeService 结构化提取教育、经历、技能与项目，并结合 JD 给出优化建议。</p></div></div>
    <div class="profile-step"><b>02</b><div><h3>候选人画像</h3><p>生成多维能力画像、技能雷达、岗位匹配度与短板识别。</p></div></div>
    <div class="profile-step"><b>03</b><div><h3>职业测评</h3><p>基于霍兰德 RIASEC 六型兴趣模型，生成兴趣代码与倾向图谱。</p></div></div>
    <div class="profile-step"><b>04</b><div><h3>职业规划</h3><p>结合测评、简历与目标岗位，生成阶段目标与行动清单。</p></div></div>
  </section>

  <section class="profile-visual">
    <ScreenFrame src="/assets/interview-analysis.png" alt="能力分析与复盘页面" position="center 24%" />
    <div class="loop-badge"><strong>数据互通</strong><span>形成持续成长闭环</span></div>
  </section>
</div>

---

<div class="eyebrow">Practice · 训练闭环</div>
<h2 class="slide-title">方案、记录与技巧库把每次面试变成下一轮训练</h2>

<div class="practice-layout">
  <section class="practice-column">
    <div class="section-label">面试方案 & 记录</div>
    <ul class="feature-list">
      <li>InterviewPlanEditor 自定义岗位、级别、时长与题型</li>
      <li>InterviewHistoryService 保存全量问答</li>
      <li>SessionBranchService 记录分支路径</li>
      <li>自动生成能力雷达与改进建议</li>
      <li>正式模拟 ≥ 70 分显示复试资格按钮</li>
    </ul>
  </section>
  <section class="practice-column warm">
    <div class="section-label">面试技巧库</div>
    <ul class="feature-list">
      <li>SkillsLibraryService 管理技巧文章</li>
      <li>按岗位与主题双维度分类</li>
      <li>覆盖高频考点与 STAR 等答题框架</li>
      <li>关联题库，实现学完即练</li>
    </ul>
  </section>
</div>

<div class="dashboard-band">
  <div>
    <small>首页仪表盘 Dashboard</small>
    <h3>概览、快捷操作、待办与成长趋势集中呈现</h3>
    <p>最近面试时间线 · 画像摘要 · 新建方案 / 开始练习 / 上传简历 · 待办提醒 · 累计练习与平均得分</p>
  </div>
  <div class="dashboard-preview"><img src="/assets/home.png" alt="首页仪表盘" /></div>
</div>

---

<div class="eyebrow">Bank · Task · Settings</div>
<h2 class="slide-title">基础工具与认证能力，让训练平台可以长期稳定使用</h2>

<div class="utility-grid">
  <section>
    <div class="utility-icon">Q</div>
    <h3>岗位题库</h3>
    <ul>
      <li>按岗位、难度与标签筛选</li>
      <li>典型岗位快捷入口</li>
      <li>标签限显 4 项并可点击筛选</li>
      <li>支持增删改试题</li>
    </ul>
  </section>
  <section>
    <div class="utility-icon">T</div>
    <h3>任务中心</h3>
    <ul>
      <li>后台任务实时进度</li>
      <li>失败可重试</li>
      <li>并发控制与队列</li>
    </ul>
  </section>
  <section>
    <div class="utility-icon">S</div>
    <h3>系统设置</h3>
    <ul>
      <li>模型选择与密钥配置</li>
      <li>亮色 / 暗色主题切换</li>
      <li>数据导出与备份</li>
    </ul>
  </section>
  <section class="auth">
    <div class="utility-icon">A</div>
    <h3>用户认证</h3>
    <ul>
      <li>AuthService 管理登录、注册与 Token</li>
      <li>bcrypt 密码加密存储</li>
      <li>JWT 自动刷新与登录态持久化</li>
      <li>普通用户 / 管理员权限分级</li>
      <li>敏感操作二次确认</li>
    </ul>
  </section>
</div>

---

<div class="eyebrow">Testing · QA</div>
<h2 class="slide-title">五类验证覆盖构建、功能、交互、对话与数据安全</h2>

<div class="qa-list">
  <div class="qa-item"><b>01</b><strong>编译构建验证</strong><p>JDK 25 + Maven 全量编译通过（BUILD SUCCESS）；Flyway 迁移脚本自动执行且版本一致。</p><span>PASS</span></div>
  <div class="qa-item"><b>02</b><strong>功能测试</strong><p>注册登录、AI 面试、知识库、简历解析、测评生成与规划导出等核心流程逐一走查。</p><span>PASS</span></div>
  <div class="qa-item"><b>03</b><strong>UI / 交互测试</strong><p>页面跳转、表单校验、列表筛选与流式对话渲染符合预期，无阻断性缺陷。</p><span>PASS</span></div>
  <div class="qa-item"><b>04</b><strong>AI 对话测试</strong><p>流式输出、追问承接、动机题穿插与超时降级等关键路径稳定不卡死。</p><span>PASS</span></div>
  <div class="qa-item"><b>05</b><strong>数据安全测试</strong><p>SQLite 与向量库持久化正确；敏感配置不落库，认证与权限校验生效。</p><span>PASS</span></div>
</div>

---

<div class="eyebrow">Traceability · 需求追溯</div>
<h2 class="slide-title">12 / 12 项功能需求已实现并完成自测</h2>

<table class="compact-table trace-table">
  <thead><tr><th>需求</th><th>实现说明</th><th>状态</th><th>需求</th><th>实现说明</th><th>状态</th></tr></thead>
  <tbody>
    <tr><td>FR-1 认证</td><td>AuthService + JWT，登录态持久化</td><td>✓ 已达成</td><td>FR-7 职业规划</td><td>阶段目标 + 行动清单</td><td>✓ 已达成</td></tr>
    <tr><td>FR-2 AI 面试</td><td>13 节点引擎 + 追问 + 动机 + 评分</td><td>✓ 已达成</td><td>FR-8 方案 / 复盘</td><td>编辑器 + 历史 + 雷达报告</td><td>✓ 已达成</td></tr>
    <tr><td>FR-3 RAG</td><td>上传 / 切片 / 向量化 / 检索注入</td><td>✓ 已达成</td><td>FR-9 岗位题库</td><td>标签 / 难度 / 岗位筛选练习</td><td>✓ 已达成</td></tr>
    <tr><td>FR-4 简历</td><td>ResumeService 结构化解析 + 建议</td><td>✓ 已达成</td><td>FR-10 技巧库</td><td>岗位 / 主题分类 + 答题框架</td><td>✓ 已达成</td></tr>
    <tr><td>FR-5 画像</td><td>技能雷达 + 匹配度 + 短板识别</td><td>✓ 已达成</td><td>FR-11 任务监控</td><td>进度条 + 重试 + 并发控制</td><td>✓ 已达成</td></tr>
    <tr><td>FR-6 测评</td><td>RIASEC 六型兴趣模型</td><td>✓ 已达成</td><td>FR-12 系统设置</td><td>模型 / 主题 / 数据配置</td><td>✓ 已达成</td></tr>
  </tbody>
</table>

<div class="completion-banner">
  <div><strong>100%</strong><span>需求完成率</span></div>
  <p>功能需求均已实现并完成关键路径自测，形成从需求到实现的完整追溯链。</p>
</div>

---

<div class="eyebrow">Innovation</div>
<h2 class="slide-title">六个创新点，让系统兼具体验、业务价值与工程质量</h2>

<div class="innovation-list">
  <div><b>01</b><strong>拟人化面试流</strong><p>13 节点状态机模拟追问、承接、转折与收尾。</p></div>
  <div><b>02</b><strong>技术深挖 + 动机穿插</strong><p>硬实力验证与职业动机评估双轨并行。</p></div>
  <div><b>03</b><strong>RAG 岗位知识增强</strong><p>本地向量检索注入岗位上下文，让面试官真正懂业务。</p></div>
  <div><b>04</b><strong>一体化成长闭环</strong><p>面试、测评、规划与简历优化在同一应用内联动。</p></div>
  <div><b>05</b><strong>本地优先，隐私可控</strong><p>数据保存在 SQLite 与本地向量库，离线可用。</p></div>
  <div><b>06</b><strong>工程化生产级结构</strong><p>Flyway、MyBatis、后台任务与异常降级提升可维护性。</p></div>
</div>

---

<div class="eyebrow">Conclusion</div>
<h2 class="slide-title">项目已完成全部需求，具备完整、可靠、可持续演进的交付基础</h2>

<div class="conclusion-claim">
  <p>本项目已按需求规格完成全部 12 项功能需求（FR-1 ～ FR-12）与各项非功能需求。系统功能完整、运行稳定、界面友好、数据私有安全，工程结构规范可维护。</p>
</div>

<div class="conclusion-grid">
  <div><b>✓</b><h3>功能完整</h3><p>14 个模块全部实现，需求完成率 100%</p></div>
  <div><b>✓</b><h3>质量可靠</h3><p>全量编译通过，关键路径自测稳定，数据本地安全可控</p></div>
  <div><b>✓</b><h3>特色突出</h3><p>拟人化多智能体面试 + RAG 知识增强 + 一体化成长闭环</p></div>
</div>

<div class="closing-line">
  <span>汇报日期：2026.07</span>
  <strong>感谢各位评审！</strong>
</div>
