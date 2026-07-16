# AI Interviewer 开发状态

最后更新：2026-07-16

## 当前结论

MVP 业务主流程已经贯通：本地账户 → 简历解析 → 候选人画像确认 → 面试方案 → AI 面试与暂停恢复 → 后台评分 → 面试记录与报告。

当前主线进入 Windows 发布验收阶段。局部体验优化不得阻塞发布链验证，除非问题会造成数据丢失、安全风险或主流程不可用。

## S1 开发状态

开发分支：`feature/inin-s1`

### Phase S1-1：领域知识与主张验证

- [x] DomainPack：内置 Java 后端、全栈工程师和产品经理岗位包；启动时完成结构校验、SQLite 版本化同步与 Lucene 索引重建。
- [x] 面试方案绑定 DomainPack；创建会话时冻结知识包 ID、版本和完整快照，历史会话不受内置包升级影响。
- [x] ClaimExtractorNode、InterviewClaim 与 ClaimLedger：每轮回答先提取原子主张，严格校验 JSON Schema，失败自动修复一次后安全降级；主张按用户/会话持久化并写入版本化 Checkpoint。
- [x] ProbePlannerNode 与 QuestionRendererNode：先按主张重要度、可信度与证据缺口生成结构化追问目标和策略，再由独立 Renderer 渲染单个自然语言问题；阶段首题与主张追问分流，计划写入 V2.1 Checkpoint。

### 后续阶段

- [ ] Phase S1-2：逻辑链与证据评分
- [ ] Phase S1-3：跨轮一致性验证
- [ ] Phase S1-4：压力控制与动态场景
- [ ] Phase S1-5：Persona 与问题质量审查
- [ ] Phase S1-6：增强报告、分支复盘与训练闭环

最近一次 S1 验证（2026-07-16）：S1-1 全部完成；DomainPack 加载/索引/冻结，ClaimExtractor 修复降级、Claim Ledger 幂等持久化与用户隔离，以及 Probe Planner 的具体主张优先级/策略映射、Question Renderer 约束、Checkpoint V1/V2.0→V2.1 升级和本地完整 TestFX 业务流程测试通过；数据库迁移基线为 V16。

## 主流程状态

- [x] 本地账户、SQLite 迁移和用户数据隔离
- [x] 简历上传、后台解析、画像生成与人工确认
- [x] 知识文档处理、Embedding、Lucene 用户索引和会话范围冻结
- [x] 面试方案、画像/知识关联和会话快照
- [x] 流式 AI 面试、Checkpoint、暂停恢复和失败内容保留
- [x] 最终回答幂等保存、后台报告生成、自动重试和重启恢复
- [x] 面试记录、完整问答、引用定位和 Markdown 报告
- [x] 全局任务中心、任务生命周期反馈、失败重新排队和终态任务安全删除
- [x] 本地确定性 TestFX：注册登录 → 简历解析 → 画像生成确认 → 知识解析/Embedding/Lucene → 三题 Agent 面试/RAG 引用 → 六维评分报告
- [x] 真实 Provider TestFX：使用环境变量完成真实画像、Embedding、语义检索、单题流式面试和评分报告
- [x] 真实画像专项测试：完整全栈简历经后台任务、JSON 流式 API 与落库链路；单次 Provider 尝试可观测
- [x] JaCoCo 覆盖率报告与最低门槛：总体行覆盖率不低于 70%，分支覆盖率不低于 45%
- [x] `jpackage` app-image、便携 ZIP、EXE/MSI 构建脚本
- [x] 当前代码构建 app-image，并通过首次启动与同目录重启数据保留冒烟
- [x] 当前代码实际生成开发版 EXE/MSI，并生成 SHA-256 校验清单
- [ ] 在全新 Windows 用户环境执行安装、快捷方式、卸载和升级保留数据验收
- [ ] 发布前补充 ICO 与代码签名；开发阶段不阻塞主流程

## 局部优化清单（不阻塞当前主线）

### 任务与通知

- 任务中心增加类型/状态筛选、搜索和历史任务归档。
- 连续完成多个任务时合并活动回执，并提供可回看的通知历史。
- 报告生成完成回执直接进入业务报告，而不先经过任务详情。
- 任务详情展示业务对象名称，例如简历名、知识文档名和面试方案名。

### 页面体验

- 首页替换静态能力说明，展示真实的简历、方案、面试和报告统计。
- 将简历画像页面的定时轮询统一替换为任务事件订阅。
- 为长列表补充分页或虚拟滚动策略，并统一保留筛选条件。
- 继续完善键盘操作、焦点可见性、读屏标签和最小窗口缩放。
- 在主流程稳定后补齐暗色主题。

### 组件与一致性

- 抽取任务类型、面试状态等 UI 文案映射，减少 Controller 内重复 `switch`。
- 将 Loading、Empty、Error 状态进一步统一为可复用组件。
- 统一表格状态标签、危险操作确认文案和非模态成功反馈。
- 根据最终 ICO 调整应用品牌标识和安装器视觉。

### 工程与测试

- 降低预期异常测试产生的堆栈日志噪声。
- 继续补充 UI 控制器的错误、取消、删除、暂停恢复和失败重试分支；当前控制器行覆盖率为 61.9%。
- 对真实 Provider 增加 429 限流和持续网络中断验收；当前已覆盖 DNS/超时根因诊断、凭据脱敏和后台任务重试。
- 发布前在干净 Windows 账户完成安装器升级/卸载人工验收记录。

## 明确延期

V1 不实现云同步、OAuth、语音、OCR、本地模型、代码运行、自动更新和多 AI Provider。除非产品范围变更，不应提前占用当前主线开发时间。

## 验证基线

- 开发构建：`mvnw clean verify`
- 本地 TestFX：`mvnw test-compile "-Dit.test=CompleteBusinessFlowE2ETest" failsafe:integration-test failsafe:verify`
- 真实 Provider TestFX：设置 `AI_LLM_LIVE_TEST=true` 后运行 `mvnw test-compile "-Dit.test=LiveProviderBusinessFlowE2ETest" failsafe:integration-test failsafe:verify`
- 真实画像专项测试：设置 `AI_LLM_LIVE_TEST=true` 后运行 `mvnw "-Dtest=LiveCandidateProfileApiIntegrationTest" test`
- 覆盖率报告：`target/site/jacoco/index.html`
- app-image：`packaging\windows\Build-Package.ps1 -Type app-image`
- 发布冒烟：`packaging\windows\Test-AppImage.ps1`
- 真实 AI 测试：仅在显式设置 `AI_LLM_LIVE_TEST=true` 时运行

最近一次发布验证（2026-07-13）：app-image 构建成功；首次启动完成数据库迁移并启动 2 个后台 Worker；使用同一 `AI_INTERVIEWER_HOME` 重启成功，数据库与外部用户文件保持不变；开发版 EXE/MSI 均已生成并写入 SHA-256 清单。

最近一次本地业务流程验证（2026-07-15）：TestFX 使用临时用户目录、配置式简历/知识路径、确定性 AI 与 Embedding，实现注册登录、全栈简历解析、画像生成确认并返回简历中心、知识切片与 Lucene 索引、语义检索、画像/知识会话快照、最大化面试、对话自动滚动、合法阶段切换、追问、两轮 RAG 引用、三题问答、报告验收和终态任务删除；不访问真实 Provider。

最近一次真实 Provider 验证（2026-07-15）：定位到 Spring AI OpenAI 适配器默认 3 次内部重试与后台任务 3 次重试叠加，旧版 60 秒超时会将一次画像操作放大为最多 12 次 HTTP 尝试；同时 `deepseek-ai/DeepSeek-V4-Pro` 默认思考模式在完整简历画像请求中超过 5 分钟。修复后 SDK 内部重试关闭，SiliconFlow DeepSeek V4 默认关闭思考，完整全栈简历专项测试通过；测试用例耗时 45.7 秒（Maven 总耗时约 65 秒），画像由单次 Provider 请求成功生成并落库。

最近一次稳定覆盖率基线（2026-07-16）：使用 `D:\Libs\Java\jdk-21.0.2` 执行 `mvnw clean verify` 通过；Surefire 84 项（82 通过、2 个真实测试跳过），Failsafe 2 项（本地 TestFX 通过、真实 TestFX 跳过）。总体行覆盖率 78.7%、分支覆盖率 53.0%，均高于 70%/45% 门槛。真实 Provider 测试不计入稳定覆盖率基线。


## 自测问题记录

1. [x] 后台任务无法删除：已支持从任务列表和详情页逻辑删除已完成/失败任务；排队中和执行中的任务禁止删除，避免业务状态残缺。
