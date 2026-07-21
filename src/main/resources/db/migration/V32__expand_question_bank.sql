-- ============================================================
-- V24：大幅扩充面试题库（从 14 道 → 90+ 道）
-- 新增 4 个岗位 + 为全部 8 个岗位补充丰富题目
-- 新增标签 + 为所有题目（含 V19 原有 14 道）建立标签关联
-- 难度枚举仅使用：JUNIOR / MEDIUM / SENIOR / EXPERT
-- ============================================================

-- ============ 一、新增岗位（4 个） ============
INSERT INTO job_position(user_id, title, department, description) VALUES
(1, '测试开发工程师', '研发中心', '负责测试框架搭建与自动化测试，熟悉接口/UI/性能测试，具备白盒测试与持续集成能力。'),
(1, '运维工程师', '技术保障部', '负责系统部署、监控告警、容器化与 CI/CD 流水线，保障线上服务高可用与故障快速恢复。'),
(1, 'UI/UX 设计师', '设计中心', '负责产品界面设计与用户体验优化，精通设计工具与设计规范，具备用户研究与交互设计能力。'),
(1, '全栈工程师', '研发中心', '同时掌握前端与后端开发，能独立完成从界面到数据库的全栈功能交付，熟悉多种技术栈。');

-- ============ 二、新增标签 ============
INSERT OR IGNORE INTO question_tag(user_id, name) VALUES
(1, '测试'), (1, '自动化'), (1, '性能测试'), (1, 'Docker'), (1, 'Kubernetes'),
(1, 'Linux'), (1, 'CI/CD'), (1, '监控'), (1, '设计规范'), (1, '用户体验'),
(1, 'Figma'), (1, 'Node.js'), (1, 'Python'), (1, 'Redis'), (1, '微服务'),
(1, '消息队列'), (1, 'JVM'), (1, '网络协议'), (1, '安全'), (1, '项目管理'),
(1, '领导力'), (1, '学习能力'), (1, '抗压能力'), (1, '机器学习'),
(1, '数据可视化'), (1, 'A/B 测试'), (1, '需求分析'), (1, '原型设计');

-- ============ 三、扩充面试题 ============

-- ==================== Java 后端开发工程师（新增 10 道） ====================
INSERT INTO interview_question(user_id, job_id, category, title, content, reference_answer, difficulty) VALUES
-- 技术题
(1, (SELECT id FROM job_position WHERE title='Java 后端开发工程师' AND user_id=1),
 'TECHNICAL', 'Java 线程池的核心参数与拒绝策略',
 '请详细说明 ThreadPoolExecutor 的 7 个核心参数，以及 4 种内置拒绝策略的适用场景。',
 '核心参数：corePoolSize（核心线程数）、maximumPoolSize（最大线程数）、keepAliveTime（空闲线程存活时间）、unit（时间单位）、workQueue（任务队列）、threadFactory（线程工厂）、handler（拒绝策略）。4 种策略：AbortPolicy（默认，抛异常）、CallerRunsPolicy（调用者线程执行）、DiscardOldestPolicy（丢弃队列最旧任务）、DiscardPolicy（静默丢弃）。实际中常用自定义策略如提交到 MQ 或记录日志。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='Java 后端开发工程师' AND user_id=1),
 'TECHNICAL', 'MySQL 索引失效的常见场景',
 '在哪些情况下 MySQL 的索引会失效？如何通过 Explain 分析索引使用情况？',
 '失效场景：1) 对列使用函数或计算；2) LIKE 以%开头；3) OR 连接中有未索引列；4) 隐式类型转换；5) 联合索引不满足最左前缀；6) != / NOT IN；7) IS NULL / IS NOT NULL（视引擎版本）。Explain 关键字段：type（访问类型，ALL 最差）、key（实际用到的索引）、rows（预估扫描行数）、Extra（Using filesort/temporary 等警告）。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='Java 后端开发工程师' AND user_id=1),
 'TECHNICAL', 'Redis 缓存穿透/击穿/雪崩及解决方案',
 '请解释 Redis 缓存穿透、缓存击穿和缓存雪崩的区别，并分别给出解决方案。',
 '穿透：查询不存在数据 → 布隆过滤器 + 缓存空值（短 TTL）。击穿：热点 Key 过期 → 互斥锁重建 + 逻辑过期（不设物理 TTL）。雪崩：大量 Key 同时过期 → TTL 加随机值 + 多级缓存 + 熔断降级。三者区别：穿透是查不存在的数据、击穿是单点热点过期、雪崩是批量过期。',
 'SENIOR'),

(1, (SELECT id FROM job_position WHERE title='Java 后端开发工程师' AND user_id=1),
 'TECHNICAL', '分布式事务的实现方案对比',
 '在微服务架构中，如何保证跨服务的分布式事务一致性？对比至少两种方案。',
 '1) 两阶段提交（2PC/Seata AT）：强一致但性能差，阻塞资源。2) TCC（Try-Confirm-Cancel）：最终一致，业务侵入性强，需实现三个接口。3) 本地消息表（基于 MQ 的最终一致）：解耦好、实现简单但需处理幂等。4) Saga：长事务拆分多个本地事务，补偿回滚。选择依据：对一致性要求 vs 业务复杂度 vs 性能容忍度。',
 'EXPERT'),

(1, (SELECT id FROM job_position WHERE title='Java 后端开发工程师' AND user_id=1),
 'TECHNICAL', 'JVM 内存模型与垃圾回收',
 '请描述 JVM 运行时数据区的结构，以及 CMS 和 G1 回收器的工作原理和适用场景。',
 '运行时数据区：堆（新生代 Eden+S0+S1 + 老年代）、栈（线程私有）、方法区（元空间）、程序计数器。CMS：标记-清除，低停顿但有碎片和浮动垃圾问题（JDK 9 废弃）。G1：Region 划分 + RSet + SATB，混合回收，可预测停顿（-XX:MaxGCPauseMillis），JDK 9+ 默认。选择：延迟敏感用 G1/ZGC，吞吐优先用 Parallel Scavenge。',
 'SENIOR'),

(1, (SELECT id FROM job_position WHERE title='Java 后端开发工程师' AND user_id=1),
 'SCENARIO', '设计一个秒杀系统',
 '如果让你设计一个电商秒杀系统（10 万商品，100 万并发请求），你会如何设计？',
 '分层设计：1) 网关层：限流（令牌桶）、黑名单、风控；2) 服务层：Redis 预扣库存（Lua 脚本原子操作）、MQ 异步削峰下单；3) 数据库层：分库分表、乐观锁更新库存；4) 前端：静态化、按钮防重复点击、CDN。关键点：超卖控制、热点防护、降级熔断、数据一致性（最终一致）。',
 'EXPERT'),

(1, (SELECT id FROM job_position WHERE title='Java 后端开发工程师' AND user_id=1),
 'BEHAVIORAL', '技术选型的决策过程',
 '请描述一次你在项目中做重要技术选型（如框架、中间件）的经历，你是怎么评估和决策的？',
 'STAR 作答：项目背景与技术需求（情境）、候选方案调研与对比维度（任务）、POC 验证/团队评审/成本风险评估（行动）、最终选择理由与后续效果复盘（结果）。考察：是否全面考虑了性能/生态/团队能力/维护成本，而非盲目追新。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='Java 后端开发工程师' AND user_id=1),
 'BEHAVIORAL', '如何指导初级工程师成长',
 '作为团队中的高级成员，你是如何帮助新人快速融入团队并提升技术能力的？',
 'STAR 作答：新人背景与团队现状（情境）、培养目标（任务）、具体措施如代码 Review 指导、分配渐进式任务、定期 1on1、分享最佳实践（行动）、新人的成长表现与团队整体提升（结果）。体现导师思维与沟通能力。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='Java 后端开发工程师' AND user_id=1),
 'SCENARIO', '数据库慢 SQL 排查与优化',
 '线上某接口响应时间从 200ms 慢到 5s，怀疑是数据库问题，你如何系统性排查和优化？',
 '步骤：1) 开启慢日志定位具体 SQL；2) Explain 分析执行计划（type/key/rows/Extra）；3) 常见优化：加索引、改写 SQL 避免 JOIN/子查询、分页优化（深分页用游标）、避免 SELECT *；4) 架构层面：读写分离、分库分表、引入缓存；5) 持续监控：PT-query-digest 或 PMM。',
 'SENIOR'),

(1, (SELECT id FROM job_position WHERE title='Java 后端开发工程师' AND user_id=1),
 'TECHNICAL', 'Spring Boot 自动装配原理',
 'Spring Boot 的自动配置（Auto Configuration）是如何实现的？@SpringBootApplication 注解背后的工作原理？',
 '@SpringBootApplication = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan。核心：@EnableAutoConfiguration 通过 @Import(AutoConfigurationImportSelector.class) 加载 META-INF/spring.factories（或 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports）中声明的配置类，结合 @ConditionalOnClass/@ConditionalOnProperty 等条件注解按需生效。自定义 Starter 也遵循此机制。',
 'MEDIUM');

-- ==================== 前端开发工程师（新增 10 道） ====================
INSERT INTO interview_question(user_id, job_id, category, title, content, reference_answer, difficulty) VALUES
(1, (SELECT id FROM job_position WHERE title='前端开发工程师' AND user_id=1),
 'TECHNICAL', 'React Hooks 与 Vue 3 Composition API 对比',
 '请对比 React Hooks（useState/useEffect）与 Vue 3 Composition API（ref/reactive）的设计思想差异和使用体验。',
 '相同点：都是函数式组合逻辑、解决类组件/Options API 的逻辑复用难题。差异：1) React Hooks 依赖数组显式声明触发条件，Vue 基于 Proxy 自动追踪依赖更"智能"；2) React 每次 render 创建新闭包需注意 stale closure，Vue ref 值始终引用同一对象；3) React Hooks 规则（不嵌套/不在条件中调用），Vue 无此限制；4) 生态系统：React hooks 库丰富，Vue Composition API 与 Options API 可混用过渡平滑。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='前端开发工程师' AND user_id=1),
 'TECHNICAL', '前端安全：XSS 与 CSRF 防御',
 '请解释 XSS（跨站脚本攻击）和 CSRF（跨站请求伪造）的原理，并说明在前端和后端应如何防御。',
 'XSS：恶意脚本注入页面 → 防御：输出转义、CSP（Content-Security-Policy）、HttpOnly Cookie、输入校验。CSRF：利用用户已登录身份发起伪造请求 → 防御：SameSite Cookie、CSRF Token（请求头携带）、验证 Referer/Origin。两者区别：XSS 是注入执行恶意代码，CSRF 是冒用用户身份发合法请求。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='前端开发工程师' AND user_id=1),
 'TECHNICAL', '虚拟 DOM 的原理与 diff 算法',
 '什么是虚拟 DOM？它的 diff 算法大致流程是怎样的？虚拟 DOM 一定比直接操作 DOM 快吗？',
 'vDOM 是 JS 对象树表示真实 DOM 结构。diff 过程：同层比较（只跨层级移动不深入）、类型不同则替换整棵子树、Key 列表 diff（同 key 复用/移动/新增/删除）。不一定更快——vDOM 的优势在于批量更新减少重排、跨平台渲染（Native/Canvas/SVG）、声明式编程模型。对于少量 DOM 操作直接操作反而更快。核心价值是编程体验和可维护性，非性能本身。',
 'SENIOR'),

(1, (SELECT id FROM job_position WHERE title='前端开发工程师' AND user_id=1),
 'TECHNICAL', 'Webpack 打包优化实践',
 '一个大型 SPA 项目打包后体积过大（bundle 超 5MB），你会从哪些方面进行构建优化？',
 '代码层面：Tree Shaking（ES Module）、代码分割（SplitChunks 动态 import）、Scope Hoisting。加载层面：路由懒加载、第三方库 CDN 外部化、gzip/brotli 压缩。缓存层面：内容哈希文件名、runtime 提取。分析工具：webpack-bundle-analyzer 定位大模块。进阶：升级到 Vite/Rspack 利用原生 ESM 提升开发体验。',
 'SENIOR'),

(1, (SELECT id FROM job_position WHERE title='前端开发工程师' AND user_id=1),
 'SCENARIO', '设计一套组件库的架构',
 '如果你来设计一套企业级 UI 组件库，你会从哪些方面规划？请给出架构设计和关键实现思路。',
 '规划维度：1) 设计：Design Token（颜色/间距/字体/圆角）统一视觉语言，组件规范文档；2) 工程化：Monorepo 管理（pnpm workspace）、TypeScript 严格模式、Rollup 多格式构建（ESM/CJS/UMD）；3) 质量：单元测试（Vitest/Jest）、Visual Regression Test（Chromatic）、a11y 无障碍；4) 文档：VitePress/Storybook 交互式演示；5) 发布：语义化版本、变更日志自动生成。组件设计原则：受控/非受控、Composition over Inheritance、Renderless 模式。',
 'EXPERT'),

(1, (SELECT id FROM job_position WHERE title='前端开发工程师' AND user_id=1),
 'BEHAVIORAL', '推动团队采用 TypeScript',
 '你的团队一直在用 JavaScript 写业务代码，你觉得应该迁移到 TypeScript，你如何说服团队并落地？',
 'STAR 作答：JS 类型缺失导致的历史 Bug和维护痛点（情境）、TS 迁移目标（任务）、策略：先新代码强制 TS、旧文件逐步改造、提供 tsconfig 最佳实践模板、组织内部分享 TS 优势、建立 Code Review 规范（行动）、迁移后的 bug 率下降和开发效率提升（结果）。体现影响力与技术判断力。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='前端开发工程师' AND user_id=1),
 'BEHAVIORAL', '处理产品经理的不合理需求',
 '当产品经理提出一个你认为技术上不合理或性价比很低的需求时，你会怎么处理？',
 '先理解需求本质（可能只是表达方式问题），然后用数据和原型提供替代方案：1) 说明技术成本（工期/风险/维护性）；2) 给出 2-3 个替代方案及各自优劣；3) 用 MVP 思维建议先做核心功能验证价值；4) 若仍坚持则记录并存档（Cover Your Ass），后续用数据说话。关键是态度协作而非对抗。',
 'JUNIOR'),

(1, (SELECT id FROM job_position WHERE title='前端开发工程师' AND user_id=1),
 'SCENARIO', '移动端 H5 适配方案',
 '需要开发一个在 iOS 和 Android 上都能良好展示的 H5 活动页，你会如何处理屏幕适配和兼容性问题？',
 '适配方案：rem/vw（postcss-px-to-viewport/vw 插件自动转换）+ viewport meta 标签（width=device-width）。兼容性：iOS 输入框弹起页面不回弹（scrollIntoView）、Android 软键盘顶起布局（visualViewport API）、1px 边框（transform: scale）、passive 事件监听、视频自动播放限制。工具：eruda 移动端调试、BrowserStack 云真机测试。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='前端开发工程师' AND user_id=1),
 'TECHNICAL', 'CSS Grid 与 Flexbox 的选择场景',
 '请说明 CSS Grid 和 Flexbox 各自适合什么布局场景，并举出实际例子。',
 'Flexbox：一维布局（行或列），适合导航栏、卡片列表、居中对齐、等分布局。Grid：二维布局（行+列），适合整个页面骨架、仪表盘、画廊、复杂表单。典型搭配：外层 Grid 定义区域（header/sidebar/main/footer），内部 Flexbox 处理各区域的子元素排列。Grid 还支持 minmax()、auto-fit/fr 自适应列数，是现代 CSS 布局的首选。',
 'JUNIOR'),

(1, (SELECT id FROM job_position WHERE title='前端开发工程师' AND user_id=1),
 'SCENARIO', '长列表渲染性能优化',
 '一个列表有上万条数据，直接渲染会导致页面卡顿，你有哪些优化方案？',
 '方案一：虚拟滚动（react-window/vue-virtual-scroller），只渲染可视区内的几十个 DOM 节点。方案二：分页/无限滚动，按需加载。方案三：时间切片（requestIdleCallback）分批渲染避免阻塞主线程。其他：冻结对象 Object.freeze 减少响应式开销（Vue）、列表 key 使用稳定唯一值避免不必要的 diff。虚拟滚动是最通用有效的方案。',
 'MEDIUM');

-- ==================== 产品经理（新增 8 道） ====================
INSERT INTO interview_question(user_id, job_id, category, title, content, reference_answer, difficulty) VALUES
(1, (SELECT id FROM job_position WHERE title='产品经理' AND user_id=1),
 'SCENARIO', 'PRD（产品需求文档）的完整结构',
 '一份完整的 PRD 应该包含哪些模块？每个模块的关键产出是什么？',
 '标准 PRD 结构：1) 文档背景与目标（为什么做、成功指标）；2) 用户角色与使用场景（Persona）；3) 功能需求详述（功能清单、流程图、状态机、异常路径）；4) 非功能性需求（性能/安全/兼容性）；5) 数据埋点与统计需求；6) UI 原型与交互说明；7) 上线计划与验收标准。核心原则：让开发和测试看完就能开工和验收，不留歧义。',
 'JUNIOR'),

(1, (SELECT id FROM job_position WHERE title='产品经理' AND user_id=1),
 'SCENARIO', '需求优先级的排序方法',
 '手上有 20 个来自各方的需求，但下个版本只能做 5 个，你怎么排优先级？',
 '方法论：1) KANO 模型（基本型→期望型→兴奋型）；2) RICE 评分（Reach 影响范围 × Impact 影响程度 × Confidence 置信度 / Effort 工作量）；3) 四象限法（重要紧急/重要不紧急/不重要紧急/不重要不紧急）。实际做法：先过滤掉无数据支撑的需求，再与业务方对齐目标（北极星指标导向），最后用 RICE 量化排序。关键：敢于说"不"，并用数据和框架解释原因。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='产品经理' AND user_id=1),
 'BEHAVIORAL', '应对需求变更',
 '开发进行到一半，老板突然说这个方向要调整，你怎么办？',
 '第一步：冷静确认变更范围和原因（战略调整还是临时想法）。第二步：评估影响（已投入工作量、延期风险、合同/承诺）。第三步：给选项而非拒绝：A 全部变更重新排期；B 分两期，本期收尾+下期转向；C 只改最小可行部分。第四步：书面确认（邮件/钉钉纪要），避免口头承诺。体现专业性和应变能力。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='产品经理' AND user_id=1),
 'TECHNICAL', 'A/B 测试的实施流程',
 '如果要为一个注册按钮的颜色做 A/B 测试，完整的实施流程是怎样的？',
 '流程：1) 提出假设（绿色按钮转化率 > 蓝色）；2) 计算样本量（置信度95%、检验功效80%、当前基准转化率、预期提升幅度）；3) 设计实验（对照组 A 原版 vs 实验组 B 改版，确保分流随机）；4) 开发埋点（曝光事件+点击事件+注册成功事件）；5) 上线观察，等待统计显著性（p < 0.05）；6) 分析结论（是否拒绝零假设）、决定全量/放弃/继续迭代。注意：SRM 检验分流均匀性、不要中途停实验。',
 'SENIOR'),

(1, (SELECT id FROM job_position WHERE title='产品经理' AND user_id=1),
 'SCENARIO', '从 0 到 1 设计一款 To B SaaS 产品',
 '如果让你从零开始设计一款面向中小企业的 CRM SaaS 产品，你会如何规划 MVP 和演进路线？',
 '阶段一（MVP）：核心闭环——客户录入→线索跟进→商机管理→成交记录，解决"客户信息散落在 Excel"的最痛点。目标 1 个月内上线。阶段二：报表看板、权限管理、导入导出、API 开放。阶段三：营销自动化、AI 客户画像、移动端适配。定价策略：Freemium 免费版引流 + 专业版按席位收费。关键：To B 重在决策链理解（使用者≠付费者）和服务体系（实施/培训/客服）。',
 'EXPERT'),

(1, (SELECT id FROM job_position WHERE title='产品经理' AND user_id=1),
 'BEHAVIORAL', '与开发团队的冲突处理',
 '开发团队觉得你的需求频繁变更、文档写得不清楚，对你有意见，你怎么改善关系？',
 '自我反思：是否确实存在变更频繁的问题？改进：1) 需求评审会充分讨论，会后锁定需求范围（变更走正式流程）；2) PRD 补充流程图和原型减少文字歧义；3) 建立"需求澄清窗口期"，开发开始前集中答疑；4) 定期 1on1 听取反馈，承认不足并展示改进动作。关系修复靠行动而非辩解。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='产品经理' AND user_id=1),
 'TECHNICAL', '竞品分析的框架和方法',
 '请描述一次完整的竞品分析应该包含哪些维度，以及如何将分析结果转化为产品策略。',
 '分析维度：1) 市场定位（目标用户/定价/商业模式）；2) 功能矩阵（核心功能对比表格）；3) 用户体验（交互流程/视觉风格/信息架构）；4) 技术架构（性能/安全性/扩展性）；5) 运营策略（获客渠道/留存手段/社区运营）。分析方法：可用 SWOT 总结，用 MECE 拆解。转化为策略：找到差异化切入点（对手弱项=我们机会）、避开红海赛道、学习对手优秀实践。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='产品经理' AND user_id=1),
 'SCENARIO', '用户留存率下降的分析与对策',
 '某产品的次周留存率从 40% 下降到了 25%，作为产品负责人你会如何分析和应对？',
 '分析路径：1) 确认下降幅度是否具有统计显著性（排除波动）；2) 分群拆解：新/老用户、获客渠道、平台（iOS/Android/Web）、用户属性；3) 漏斗分析：哪一步流失率异常升高；4) 用户调研：问卷/电话访谈了解流失用户的真实原因；5) 结合产品变动：近期是否有版本更新/运营活动调整。对策：针对根因制定 quick win（如修复某个 crash）和中长期策略（如优化新手引导）。',
 'SENIOR');

-- ==================== 数据分析师（新增 8 道） ====================
INSERT INTO interview_question(user_id, job_id, category, title, content, reference_answer, difficulty) VALUES
(1, (SELECT id FROM job_position WHERE title='数据分析师' AND user_id=1),
 'TECHNICAL', 'Python pandas 数据清洗实战',
 '给定一个包含缺失值、异常值、重复数据的 DataFrame，请描述完整的数据清洗流程和常用方法。',
 '流程：1) 了解数据：df.info()、df.describe()、df.dtypes；2) 缺失值：df.isnull().sum() 统计，处理方式——删除（dropna）、填充（fillna，均值/中位数/前后值/插值）、标记为单独类别；3) 重复值：df.duplicated() 检测，drop_duplicates() 去重；4) 异常值：IQR 方法（Q1-1.5*IQR / Q3+1.5*IQR）、Z-score（|z|>3）、可视化（箱线图/散点图）；5) 数据类型转换：astype()、pd.to_datetime()；6) 特征工程：分箱/编码/标准化。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='数据分析师' AND user_id=1),
 'TECHNICAL', '常见的统计学假设检验方法',
 '请列举常用的假设检验方法，并说明每种方法的适用场景和判断依据。',
 '常用方法：1) t 检验：两组均值比较（独立样本/配对样本），小样本正态分布；2) 卡方检验：分类变量独立性检验（如性别与购买偏好是否相关）；3) ANOVA：多组均值比较（F 检验）；4) Mann-Whitney U：非参数替代 t 检验；5) KS 检验：分布一致性检验。判断依据：计算 p 值，若 p < α（通常 0.05）则拒绝原假设。注意：统计显著 ≠ 业务显著，需结合效应量判断。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='数据分析师' AND user_id=1),
 'SCENARIO', '搭建用户行为数据分析体系',
 '如果你加入一家电商公司，需要从零搭建用户行为数据分析体系，你会怎么做？',
 '规划：1) 数据采集：SDK 埋点（页面浏览/点击/曝光/停留时长），定义 event + params 规范；2) 数仓建模：ODS 原始层 → DWD 明细层（事件宽表）→ DWS 汇总层（日活/留存/转化漏斗）→ ADS 应用层（BI 看板）；3) 核心指标：AARRR 全漏斗 + 关键过程指标；4) 可视化：TableDB/Metabase/自研 BI 搭建实时看板；5) 产出：周报/专题分析/归因模型。工具栈：SQL + Python(pandas) + 可视化工具。',
 'SENIOR'),

(1, (SELECT id FROM job_position WHERE title='数据分析师' AND user_id=1),
 'TECHNICAL', '数据可视化的图表选型指南',
 '不同的分析目的应该选用哪些图表？请举例说明。',
 '选型原则：1) 比较：柱状图（分类比较）、雷达图（多维比较）；2) 趋势：折线图（时间序列）、面积图（累积量）；3) 占比：饼图（少分类）、环形图（多分类+中心信息）、堆叠柱状图（占比趋势）；4) 分布：直方图（数值分布）、箱线图（分布+异常值）、散点图（二维分布+相关性）；5) 关系：散点矩阵、桑基图（流转关系）、热力图（二维密度）；6) 地理：地图（ choropleth / 气泡）。禁忌： Pie > 6 类、3D 图表误导、双 Y 轴混淆。',
 'JUNIOR'),

(1, (SELECT id FROM job_position WHERE title='数据分析师' AND user_id=1),
 'BEHAVIORAL', '数据结论不被业务方认可',
 '你花了两周做的分析报告，业务方说"结论我们都知道了，没有新发现"，你怎么回应？',
 '反思：是否真的只是在验证已知事实？改进方向：1) 事前对齐：分析前先与业务方确认核心问题和期望产出，避免闭门造车；2) 深挖一层：不止回答"是什么"，还要回答"为什么"和"所以呢"；3) 给出可执行建议而非仅陈述事实；4) 展示分析过程中的意外发现（即使与初始假设矛盾也很有价值）；5) 承认不足并主动提出下一轮深化方向。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='数据分析师' AND user_id=1),
 'SCENARIO', '用户流失预警模型的构建思路',
 '电商平台希望提前识别可能流失的用户以便精准召回，请描述构建流失预警模型的完整思路。',
 '流程：1) 目标定义：未来 N 天未下单/未登录 = 流失；2) 特征工程：用户基础属性 + 行为特征（最近购买频率/金额/品类偏好变化/浏览深度下降/客服投诉次数）+ 时间特征（注册时长/生命周期阶段）；3) 样本划分：时间划分（训练集/测试集按时间段切，避免数据泄露）；4) 模型选择：逻辑回归（可解释性好，业务接受度高）/ XGBoost（精度高）/ 结合 SHAP 解释特征重要性；5) 评估：AUC/precision@K/召回率；6) 应用：输出流失概率 Top N 用户名单，推送运营触达（优惠券/短信/个性化推荐）。',
 'EXPERT'),

(1, (SELECT id FROM job_position WHERE title='数据分析师' AND user_id=1),
 'TECHNICAL', 'SQL 窗口函数综合应用',
 '请举出 3 个窗口函数的实际业务案例，并写出对应的 SQL 示例。',
 '案例一：连续签到天数——用 ROW_NUMBER 配合日期差判断连续性。案例二：同比/环比增长率——LAG/OVER(PARTITION BY 商品 ORDER BY 月) 取上月数据计算环比。案例三：每个用户累计消费金额——SUM(amount) OVER(PARTITION BY user_id ORDER BY 交易时间 ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)。窗口函数优势：避免自连接、代码简洁高效，是分析师必备技能。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='数据分析师' AND user_id=1),
 'BEHAVIORAL', '向非技术人员解释数据分析结果',
 '你需要向完全没有技术背景的业务总监汇报一份复杂的数据分析报告，你会怎么做？',
 '策略：1) 先说结论（Executive Summary 一句话说清核心发现和建议）；2) 用业务语言替代技术术语（"p<0.05" → "统计上显著可信"、"特征重要性" → "影响最大的因素"）；3) 可视化辅助（一张好图胜过千言万语）；4) 回答"那又怎样？"（So What —— 对业务的实际意义）；5) 准备附录供细节追问。原则：受众决定呈现方式，目标是驱动决策而非炫技。',
 'JUNIOR');

-- ==================== 测试开发工程师（10 道） ====================
INSERT INTO interview_question(user_id, job_id, category, title, content, reference_answer, difficulty) VALUES
(1, (SELECT id FROM job_position WHERE title='测试开发工程师' AND user_id=1),
 'TECHNICAL', '自动化测试框架的设计',
 '如果让你从零设计一个 Web 接口的自动化测试框架，你会怎样规划整体架构？',
 '架构分层：1) 测试脚本体（TestCase，用 pytest/JUnit 编写）；2) 业务封装层（Page Object / API Client，封装接口调用和数据构造）；3) 基础设施层（HTTP client、数据库操作、配置管理、日志）；4) 执行引擎（CI 集成触发、并行执行、失败重试）；5) 报告层（Allure/ExtentReports 生成 HTML 报告，含截图/日志/错误堆栈）。关键技术：数据驱动（YAML/CSV 测试数据与脚本分离）、断言库灵活、Mock 服务隔离依赖。',
 'SENIOR'),

(1, (SELECT id FROM job_position WHERE title='测试开发工程师' AND user_id=1),
 'TECHNICAL', '接口测试用例设计方法',
 '对于一个用户登录接口（POST /api/login，参数：username/password/captcha），请设计全面的测试用例。',
 '用例设计：1) 功能正常：正确账号密码 → 成功登录返回 token；2) 参数校验：空参/缺少必填/超长输入/XSS 注入/SQL 注入；3) 业务规则：错误密码 n 次后锁定/验证码错误/已注销账号/未激活账号；4) 并发安全：同一账号多地登录（互踢/允许多端）；5) 性能：QPS 达标/响应时间 P99 < 500ms；6) 安全：密码传输加密、暴力破解限流、token 有效期。方法：等价类划分 + 边界值分析 + 错误推测法。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='测试开发工程师' AND user_id=1),
 'TECHNICAL', '性能测试的完整流程',
 '请描述一次完整的性能测试流程，包括指标定义、工具选择、场景设计和结果分析。',
 '流程：1) 需求明确：确定性能指标（TPS/QPS、RT 响应时间、并发用户数、CPU/内存/IO 资源水位）；2) 场景设计：基准测试（单用户）、负载测试（逐步加压）、压力测试（超过峰值）、稳定性测试（长时间运行）、尖峰测试；3) 工具：JMeter（HTTP 协议压测）、k6（Go 编写脚本更灵活）、Locust（Python 编写）、Gatling；4) 监控：Prometheus + Grafana 监控服务器资源 + APM（SkyWalking/Pinpoint）追踪链路；5) 分析：找出瓶颈（慢 SQL / 锁竞争 / GC 频繁 / 全局限流）→ 优化 → 回归验证。',
 'SENIOR'),

(1, (SELECT id FROM job_position WHERE title='测试开发工程师' AND user_id=1),
 'SCENARIO', '上线前一天发现严重 Bug',
 '明天就要上线了，今晚测试发现了一个严重的支付金额计算错误 Bug，你怎么处理？',
 '应急流程：1) 立即确认 Bug 复现条件和影响范围（是否所有支付场景/涉及金额量级）；2) 评级定级：P0 级阻断性问题必须阻塞上线；3) 同步信息：第一时间通知项目经理/开发负责人/产品经理，拉紧急群；4) 协助开发定位（提供复现步骤/日志/数据）；5) 验证修复：回归测试该功能及相关联功能；6) 上线决策：若无法彻底修复则评估回滚方案或灰度绕过。事后复盘根因预防。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='测试开发工程师' AND user_id=1),
 'BEHAVIORAL', '与开发的 Bug 争议',
 '你提了一个 Bug，开发说是"不是 Bug"或者"没法复现"从而拒绝修复，你怎么处理？',
 '处理步骤：1) 自查：确认复现步骤清晰吗？环境一致吗？是否真的是设计如此（查需求文档）；2) 补充证据：录屏/日志/数据库快照证明问题真实存在；3) 引入第三方：找产品经理确认是否符合预期需求；4) 若确认为 Bug 但修复成本高，协商降低优先级/排入下版本/加监控告警；5) 若仍有分歧升级至技术主管仲裁。保持客观，对事不对人。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='测试开发工程师' AND user_id=1),
 'TECHNICAL', 'CI/CD 中的测试流水线搭建',
 '如何在 Jenkins/GitLab CI 中搭建一条完整的自动化测试流水线？',
 '流水线阶段：1) Code Commit → 触发 webhook；2) Lint & Unit Test（开发提交时跑，< 5min，快速反馈）；3) Build（编译打包 Docker 镜像）；4) Integration Test（部署测试环境，跑接口/E2E 测试）；5) Static Analysis（SonarQube 代码质量扫描）；6) Security Scan（依赖漏洞检查 dependency-check/Snyk）；7) Deploy to Staging（预发布环境冒烟测试）；8) Production Deploy（灰度发布 + 自动化回归门禁）。关键：每阶段设置质量门禁（覆盖率阈值/严重 Bug数为0）。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='测试开发工程师' AND user_id=1),
 'SCENARIO', '测试环境的治理方案',
 '团队反映测试环境经常不稳定（数据被污染、服务不可用、配置和线上不一致），你如何系统性地治理？',
 '治理方案：1) 环境标准化：Docker Compose/Kubernetes 编排一键拉起整套环境，版本化管理 docker-compose.yml；2) 数据管理：每次回归前初始化种子数据（Flyway/Liquibase）、测试用例间数据隔离（每个用例独立数据 setUp/tearDown）；3) 配置一致性：配置中心（Nacos/Apollo）区分环境 profile、禁止硬编码；4) 稳定性监控：环境健康检查接口、定时巡检任务、服务宕动自动报警；5) 治本：推动测试环境纳入运维统一管理，与生产环境同等对待。',
 'SENIOR'),

(1, (SELECT id FROM job_position WHERE title='测试开发工程师' AND user_id=1),
 'TECHNICAL', 'Appium 移动端自动化测试',
 '请说明 Appium 自动化测试的工作原理，以及在实践中遇到的常见问题和解决方案。',
 '原理：Appium 基于 WebDriver 协议，通过 Bootstrap.jar（Android）/ XCTest（iOS）与设备通信，将 WebDriver 命令转为 UIAutomation/XCUI 操作。常见问题：1) 元素定位不稳定 → 用 AccessibilityId 替代 XPath、增加显式等待 WebDriverWait；2) 弹窗干扰（升级提示/权限弹窗）→ capability 设置 autoGrantPermissions 或全局弹窗处理；3) 输入中文 → unicodeKeyboard 方案；4) WebView/H5 混合应用 → 切换 context（NATIVE_VIEW ↔ WEBVIEW）；5) 设备碎片化 → 云测平台（Sauce Labs/云测）覆盖主流机型。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='测试开发工程师' AND user_id=1),
 'BEHAVIORAL', "测试团队的价值如何量化",
 '管理层认为测试团队是"纯成本中心"，要求你们证明自己的价值，你会怎么做？',
 '量化维度：1) 缺陷逃逸率：线上 Bug 数 / 总 Bug 数（越低越好，目标 < 5%）；2) 缺陷移除成本：Bug 发现越早修复成本越低（测试阶段发现 vs 生产环境发现的成本差）；3) 测试效率：自动化率（自动化用例占比）、回归测试耗时缩短比例；4) 风险规避：拦截过的重大事故（估算若上线造成的损失）；5) 交付加速：自动化门禁使发布频率从每月→每周/每日。用数据和行业基准对比说话。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='测试开发工程师' AND user_id=1),
 'TECHNICAL', 'Mock 在单元测试中的应用',
 '请说明 Mock 的作用和适用场景，并以 Mockito 为例展示几个典型的 Mock 用法。',
 'Mock 作用：隔离外部依赖（数据库/网络/API），使单元测试快速、稳定、可重复。适用场景：1) 依赖尚未实现（并行开发）；2) 依赖太慢（网络调用/数据库查询）；3) 边界情况难以触发（第三方 API 异常）。Mockito 示例：when(mockService.getUser(1)).thenReturn(expectedUser); verify(mockService, times(1)).getUser(1); @InjectMocks 注入 mock。注意：不要过度 Mock（测试变成了"测试 Mock 配置"），外部依赖用 TestContainer（真实 DB 容器）更好。',
 'JUNIOR');

-- ==================== 运维工程师（10 道） ====================
INSERT INTO interview_question(user_id, job_id, category, title, content, reference_answer, difficulty) VALUES
(1, (SELECT id FROM job_position WHERE title='运维工程师' AND user_id=1),
 'TECHNICAL', 'Docker 容器的底层原理',
 '请解释 Docker 容器的核心技术（Namespace、Cgroups、Union FS），以及容器与虚拟机的区别。',
 '三大技术：1) Namespace（命名空间）：PID/NET/MNT/UTS/IPC/User 隔离进程看到的资源视图；2) Cgroups（控制组）：限制 CPU/内存/IO 资源使用上限；3) UnionFS（联合文件系统）：镜像分层存储（ReadOnly layers + ReadWrite layer），copy-on-write 写时复制。与 VM 区别：VM 需要 Hypervisor + Guest OS（GB 级内存开销），容器共享 Host Kernel（MB 级开销、秒级启动）。劣势：隔离性弱于 VM（Kernel 共享、安全边界）。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='运维工程师' AND user_id=1),
 'TECHNICAL', 'Kubernetes 核心概念与实践',
 '请解释 Kubernetes 的核心架构组件（Master/Worker 节点）和关键资源对象（Pod/Service/Deployment/ConfigMap）。',
 '架构：Master 节点（API Server 集入口、etcd 存储状态、Scheduler 调度、Controller Manager 控制循环）；Worker 节点（Kubelet 管理 Pod 生命周期、kube-proxy 网络代理、Container Runtime 运行容器）。核心资源：Pod（最小调度单元，共享网络 IPC）、Deployment（声明式管理 Pod 副本+滚动更新）、Service（Pod  stable 网络入口，ClusterIP/NodePort/LoadBalancer 类型）、ConfigMap/Secret（配置和敏感信息分离）。Ingress（七层路由）+ HPA（自动水平扩缩容）。',
 'SENIOR'),

(1, (SELECT id FROM job_position WHERE title='运维工程师' AND user_id=1),
 'SCENARIO', '线上 CPU 飙高的排查思路',
 '监控系统告警某台服务器 CPU 使用率持续超过 95%，请描述你的完整排查流程。',
 '排查链路：1) top/htop 看 CPU 占用最高的进程；2) top -H -p <pid> 查看线程级占用（定位具体线程）；3) printf "%x\n" <tid> 转 16 进制，jstack <pid> | grep <tid_hex> 查看线程栈（若是 JVM 进程）；常见原因：死锁（jstack 检测）、GC 频繁（jstat -gcutil 查看 FGC/YGC 频率和耗时）、密集计算/无限循环（线程栈可见）、正则回溯（ReDoS）。非 JVM：strace -p <pid> 跟踪系统调用定位 I/O 瓶颈。解决后建立基线告警（CPU > 80% 持续 3min 告警）。',
 'SENIOR'),

(1, (SELECT id FROM job_position WHERE title='运维工程师' AND user_id=1),
 'TECHNICAL', 'Linux 常用排查命令汇总',
 '请列举 Linux 运维中你最常用的 10 个排查命令及其典型使用场景。',
 '命令清单：1) top/htop —— 实时系统资源概览；2) df -h —— 磁盘空间使用；3) du -sh * —— 目录大小排查（定位大文件）；4) netstat -tlnp/ss -tlnp —— 端口监听和网络连接；5) lsof -i :<port> —— 查看端口被谁占用；6) journalctl -u <service> --since "1 hour ago" —— systemd 服务日志；7) curl -I / wget —— HTTP 接口探测连通性；8) tcpdump -i eth0 port 80 —— 抓包分析网络问题；9) iotop —— IO 密集进程排查；10) systemctl status/restart/start —— 服务管理。',
 'JUNIOR'),

(1, (SELECT id FROM job_position WHERE title='运维工程师' AND user_id=1),
 'SCENARIO', 'CI/CD 流水线设计与实施',
 '公司目前还是手动部署，请你设计并实施一套完整的 CI/CD 流水线。',
 '设计方案：1) 版本控制：Git Flow 工作流（develop → feature → release → main）；2) CI 阶段（GitLab CI/Jenkins Pipeline）：代码提交 → Lint → 单元测试 → SonarQube 质量门禁 → 构建镜像（Docker build）→ 推送 Harbor 镜像仓库；3) CD 阶段：开发环境自动部署 → 测试环境自动部署（集成测试通过后）→ 预发布手动审批 → 生产灰度发布（金丝雀 5% → 50% → 100%）；4) 基础设施：Kubernetes 集群 + Helm Chart 包管理；5) 可观测性：ELK 日志收集 + Prometheus + Grafana 监控 + SkyWalking 链路追踪。',
 'EXPERT'),

(1, (SELECT id FROM job_position WHERE title='运维工程师' AND user_id=1),
 'BEHAVIORAL', '凌晨 3 点线上故障处理',
 '凌晨 3 点收到线上服务不可用的告警电话，你如何处理？',
 '应急流程：1) 快速评估影响面（多少用户/多少金额损失/是否有降级方案）；2) 按预案执行：先恢复服务（重启/回滚/切换备用机房）再查根因，MTTR 优先；3) 信息同步：同步相关方（技术负责人/产品/客服），客服安抚用户；4) 保留现场：备份日志/core dump 用于事后分析；5) 事后 24h 内输出故障报告（5Whys 根因分析 + 改进行动 + 复盘会议）。心态：冷静有序按 SOP 操作，不做英雄主义冒险尝试。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='运维工程师' AND user_id=1),
 'TECHNICAL', 'Prometheus 监控体系搭建',
 '请说明 Prometheus + Grafana 监控体系的四大告警类型及指标设计原则。',
 '四大告警类型：1) 即时告警（Instant）：CPU > 90% 持续 5min；2) 趋势告警（Prediction）：磁盘 3 天后将满（predict(linear(...)））；3) 心跳告警（Absent）：服务探针超过 5min 未上报；4) 黑盒告警（BlackBox）：HTTP 接口探测失败。指标设计原则：USE 方法（Utilization/Saturation Errors 针对资源）+ RED 方法（Rate/Errors/Duration 针对服务）。命名规范：{namespace}_{subsystem}_{metric}_{unit}，如 api_http_request_duration_seconds。',
 'SENIOR'),

(1, (SELECT id FROM job_position WHERE title='运维工程师' AND user_id=1),
 'TECHNICAL', 'Nginx 配置与调优',
 '请说明 Nginx 作为反向代理和负载均衡的常用配置项和高并发调优经验。',
 '核心配置：1) 反向代理：proxy_pass upstream；2) 负载均衡算法：轮询（round_robin）/加权轮询（weight）/最少连接（least_conn）/IP Hash（ip_hash）；3) 高并发优化：worker_processes auto、worker_connections 65535、use epoll、keepalive_timeout 65、gzip on、open_file_cache。4) 安全：limit_req_zone 限流、SSL/TLS 配置、隐藏版本号（server_tokens off）。5) 日志：自定义 log_format 含 upstream_response_time 用于定位后端瓶颈。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='运维工程师' AND user_id=1),
 'SCENARIO', '数据库备份与恢复策略',
 '请为公司核心 MySQL 数据库设计一套完善的备份和灾难恢复方案。',
 '方案：1) 备份策略：全量备份（每周 mysqldump --single-transaction --master-data=2）+ 增量备份（每日 binlog 备份）；2) 存储：备份文件传至异地 OSS/S3（跨可用区容灾）；3) 恢复演练：每季度做一次恢复演练（RTO < 1h、RPO < 15min 目标）；4) 验证：备份完整性校验（mysqldump --verify）、恢复到从库验证数据一致性（pt-table-checksum）；5) 文档化：恢复 SOP 手册 + 值班人员培训。进阶：搭建 MGR 主从集群 + GTID 保证高可用。',
 'SENIOR'),

(1, (SELECT id FROM job_position WHERE title='运维工程师' AND user_id=1),
 'BEHAVIORAL', '推动基础设施标准化',
 '团队中各个项目的部署方式五花八门（有的手动 scp、有的 shell 脚本、有的 Ansible），你想推行统一的 K8s 部署规范，怎么推进？',
 '推进策略：1) 先做出标杆：选一个项目试点，展示 K8s 部署的优势（效率提升数据对比）；2) 降低门槛：编写 Helm Chart 模板、一键部署脚本、详细的迁移文档和 FAQ；3) 培训赋能：组织内部技术分享、动手实验室（ Katacoda 风格的练习环境）；4) 渐进迁移：不强求一刀切，新项目强制 K8s、老项目按节奏迁移；5) 建立激励机制：迁移完成的团队获得表彰。用成果说话，减少抵触情绪。',
 'MEDIUM');

-- ==================== UI/UX 设计师（8 道） ====================
INSERT INTO interview_question(user_id, job_id, category, title, content, reference_answer, difficulty) VALUES
(1, (SELECT id FROM job_position WHERE title='UI/UX 设计师' AND user_id=1),
 'TECHNICAL', '设计系统的构建方法',
 '请描述如何从零开始构建一套企业级设计系统（Design System），包括核心模块和实施路径。',
 '核心模块：1) 设计Token（Design Tokens）：颜色（主色/中性色/语义色）/字体（字号/字重/行高）/间距（4pt 基础网格）/圆角/阴影，以 JSON/YAML 格式沉淀；2) 组件库：基础（Button/Input/Icon）、复合（Modal/Table/Form）、模板（列表页/详情页/表单页）；3) 模式文档：文案规范/图标使用/插画风格/动效原则；4) 工具链：Figma Auto Layout + Variants + Components + Design Tokens Plugin；5) 工程对接：Tokens 导出为 CSS Variables/JS 对象，确保设计与前端一致。参考：Google Material 3、Ant Design、Carbon Design System。',
 'SENIOR'),

(1, (SELECT id FROM job_position WHERE title='UI/UX 设计师' AND user_id=1),
 'SCENARIO', '设计一个复杂的后台管理系统',
 '需要为一个拥有 50+ 功能模块的企业级 SaaS 后台设计信息架构和导航体系，你会怎么做？',
 '设计方法：1) 信息架构：卡片分类法（Card Sorting）组织模块——按用户心智模型而非组织架构分组；2) 导航层级：一级 Tab（核心域）→ 二级侧边栏（功能模块）→ 三级内容区（操作区），控制在 3 层以内；3) 高频入口：快捷操作面板、最近访问、收藏功能；4) 搜索即导航：全局搜索（Command+K）直达任意功能；5) 个性化：可折叠/拖拽排序/暗色模式；6) 面包屑 + 清晰的页面标题体系防迷路。参考：Linear/Vercel Dashboard 的导航设计。',
 'EXPERT'),

(1, (SELECT id FROM job_position WHERE title='UI/UX 设计师' AND user_id=1),
 'TECHNICAL', '色彩理论与无障碍设计',
 '在设计配色方案时，如何平衡品牌感、易读性和无障碍（Accessibility）要求？',
 '配色原则：1) 对比度：正文文字与背景 WCAG AA 标准 ≥ 4.5:1，大文本 ≥ 3:1；2) 色彩数量：主色 1 种 + 辅助色 2-3 种 + 中性色阶足够，避免 > 5 种强调色造成认知负担；3) 色彩含义不作为唯一信息载体（红=错误需配合图标/文字，照顾色盲用户约 8% 男性）；4) 深色模式：非简单反转（调整饱和度和亮度，降低纯黑 #000 的刺眼感）。工具：Figma A11y 插件、Stark Contrast Checker、WAVE 浏览器插件。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='UI/UX 设计师' AND user_id=1),
 'BEHAVIORAL', '设计稿被开发"还原度低"',
 '开发做出的页面和你设计稿有明显差距，开发者说"做不到"或"没时间"，你怎么办？',
 '应对策略：1) 自查：标注是否完整（间距/字号/色值/交互状态 hover/focus/disabled）、是否提供了切图和动效参数；2) 沟通：站在开发者角度理解约束（浏览器兼容/组件库限制/工期），区分"做不到"和"不想做"；3) 协作：设计走查环节（Design Review）在开发前对齐重点、开发中及时 check-in 而非最后验收才发现问题；4) 让步排序：Must have（品牌核心体验）→ Should have（理想效果）→ Nice to have（锦上添花）；5) 建立组件库减少反复沟通成本。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='UI/UX 设计师' AND user_id=1),
 'SCENARIO', '用户注册流程的 UX 优化',
 '某 App 注册转化率只有 30%，大部分用户在填写信息环节流失，请你提出优化方案。',
 '分析与优化：1) 数据定位：漏斗分析确定在哪一步流失最多（手机号验证? 填资料? 设置密码?）；2) 减少摩擦：手机号一键授权（运营商认证）、邮箱自动补全、密码强度实时检测且允许稍后设置；3) 分步引导： Progressive Profiling——先完成核心注册（手机号+验证码），后续使用中逐步补充信息；4) 价值前置：注册前展示 App 核心价值（截图/视频/社会证明）；5) 第三方登录：微信/Apple ID 一键登录降低门槛。目标：将注册流程压缩到 < 30 秒 3 步以内。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='UI/UX 设计师' AND user_id=1),
 'TECHNICAL', '用户研究方法的选择',
 '在不同产品阶段和资源条件下，你应该选择哪些用户研究方法？',
 '方法选择矩阵：1) 发现阶段（探索需求）：用户访谈（5-8 人深度访谈）、竞品分析、实地考察（ contextual inquiry ）；2) 设计阶段（验证方案）：可用性测试（5 人即可发现 85% 问题）、卡片分类法（信息架构）、A/B 测试；3) 发布后（持续监测）：问卷调查（NPS/CSAT）、数据分析（热力图/漏斗）、 feedback 收集。资源有限时的 MVP 研究： Guerrilla Testing（咖啡厅拦访 2-3 人）+ 内部专家评审（Heuristic Evaluation，Nielsen 十大原则）。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='UI/UX 设计师' AND user_id=1),
 'SCENARIO', '设计一个金融 App 的首页',
 '为一款面向年轻人的理财 App 设计首页，需要展示资产概览、理财产品和市场动态，如何组织信息层次？',
 '信息层次设计：1) Hero 区域：总资产数字（大字号 + 趋势箭头 + 昨日收益，一眼看到核心信息）；2) 快捷操作：转入/转出/定投（底部固定 FAB 或横向快捷入口）；3) 内容推荐：持仓详情（折叠查看明细）+ 推荐产品（基于风险偏好个性化推荐不超过 3 个）+ 市场资讯（信息流卡片，图文混排）；4) 底部导航：首页/市场/交易/资产/我的（标准五 tab）。设计原则：信息降噪（每屏一个焦点）、情感化设计（涨绿跌红中国惯例 + 适度动效）、建立信任感（银行级安全背书标识）。',
 'SENIOR'),

(1, (SELECT id FROM job_position WHERE title='UI/UX 设计师' AND user_id=1),
 'BEHAVIORAL', '作品集的准备与展示',
 '在面试中展示作品集时，你应该重点讲述哪些内容才能打动面试官？',
 '作品集要点：不讲"我画了什么漂亮界面"，而讲"我解决了什么问题"。每个 case study 包含：1) 背景：业务挑战和设计目标（最好有量化指标，如"转化率提升 20%"）；2) 我的角色：独立完成还是团队协作，具体负责哪些部分；3) 过程：用户研究洞察 → 设计探索（ sketches 多方案对比）→ 最终方案 + 遗弃方案及原因（体现设计决策力）；4) 结果：上线后的数据验证 + 用户反馈 + 学到的经验教训。准备 3-4 个深度 case，涵盖不同类型项目。',
 'JUNIOR');

-- ==================== 全栈工程师（10 道） ====================
INSERT INTO interview_question(user_id, job_id, category, title, content, reference_answer, difficulty) VALUES
(1, (SELECT id FROM job_position WHERE title='全栈工程师' AND user_id=1),
 'TECHNICAL', '全栈技术选型的考量因素',
 '作为一个全栈工程师，在为新项目做技术选型时，你会如何权衡前后端技术栈的选择？',
 '选型维度：1) 业务匹配度：CRUD 管理后台选低代码/Next.js，实时协作选 WebSocket + OT 算法，AI 应用选 Python 生态；2) 团队因素：现有技术栈（降低学习成本）、招聘市场人才供给；3) 性能要求：SSR（SEO 友好）vs CSR（交互丰富）vs SSR+Hydration（Next.js/Nuxt）；4) 生态成熟度：npm 下载量、GitHub 活跃度、社区方案丰富度；5) 运维成本：Serverless 降低运维 vs 自建 K8s 灵活性。实用推荐：React/Next.js(TypeScript) + Node.js(Nest.js) + PostgreSQL + Prisma ORM + Redis + Docker。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='全栈工程师' AND user_id=1),
 'TECHNICAL', 'RESTful API 设计规范',
 '请说明 RESTful API 的设计原则，并以"用户管理"为例设计一套完整的 API 接口。',
 'RESTful 原则：1) 资源为导向（名词非动词）；2) HTTP 语义化方法（GET 查询/POST 创建/PUT 全量更新/PATCH 部分更新/DELETE 删除）；3) 状态码准确（201 Created/400 Bad Request/401 Unauthorized/403 Forbidden/404 Not Found/500 Internal Error）；4) 分页过滤排序（?page=1&size=20&sort=-createdAt&status=active）；5) 版本管理（/api/v1/users）。示例：GET /api/v1/users（列表）、POST /api/v1/users（创建）、GET /api/v1/users/{id}（详情）、PUT /api/v1/users/{id}（更新）、DELETE /api/v1/users/{id}（删除）、GET /api/v1/users/{id}/orders（关联查询）。',
 'JUNIOR'),

(1, (SELECT id FROM job_position WHERE title='全栈工程师' AND user_id=1),
 'TECHNICAL', 'WebSocket 实时通讯实现',
 '请设计一个类似在线协作文档的实时多人编辑系统，说明 WebSocket 的使用方式和数据同步策略。',
 '架构设计：1) 连接层：WebSocket（ws 库）/Socket.IO（自动降级 polling）；2) 身份鉴权：连接时传递 token → 服务端验证 → 绑定 userId 到 socket；3) 房间管理：按 documentId 分 room，socket.join(room)；4) 数据同步：OT（Operational Transformation）或 CRDT（Conflict-free Replicated Data Type）算法解决并发编辑冲突；5) 状态管理：服务端维护 Document State 做权威源，客户端 optimistic update + 冲突时服务端校正；6) 断线重连：exponential backoff + 断线期间操作队列重放。扩展：Redis PubSub 多实例广播。',
 'EXPERT'),

(1, (SELECT id FROM job_position WHERE title='全栈工程师' AND user_id=1),
 'SCENARIO', '从零搭建一个 SaaS 平台',
 '如果给你 4 周时间，从零搭建一个最小可行的 SaaS 平台（支持用户注册登录、订阅付费、核心 CRUD 功能），你会如何规划和实现？',
 '四周计划：W1——基建：项目脚手架（Next.js + Nest.js 双 monorepo）、数据库设计（PostgreSQL + Prisma）、Docker Compose 本地开发环境、CI/CD 基础流水线；W2——核心：JWT 认证（Access+Refresh token）、OAuth2 第三方登录（Google/GitHub）、用户 CRUD + 权限 RBAC（casbin）；W3——商业：Stripe/支付宝集成（订阅计划管理、Webhook 异步处理）、用量配额（Rate Limiting）；W4——打磨：邮件服务（SendGrid/Resend）、日志监控（Pino + Datadog）、文档站（VitePress）、部署上线（Vercel + Railway/Railway）。MVP 原则：每周末产出一个可演示的版本。',
 'EXPERT'),

(1, (SELECT id FROM job_position WHERE title='全栈工程师' AND user_id=1),
 'BEHAVIORAL', '全栈工程师的深度 vs 广度',
 '有人说全栈工程师"样样通样样松"，你怎么看待这个问题？你如何在自己的技术成长中平衡广度和深度？',
 '个人观点：全栈 ≠ 每个领域都浅尝辄止，而是"T 型"人才——广度覆盖全链路（能端到端交付），同时在 1-2 个领域有深度（成为团队 go-to person）。平衡策略：1) 基础层要深：数据结构与算法、计算机网络、操作系统、数据库原理——这些不变的知识是所有技术栈的基础；2) 应用层要广：前端/后端/DevOps/数据库都要能用；3) 选方向深耕：根据兴趣和项目需要选一个方向深钻（如我选分布式系统和前端性能）；4) 保持好奇心：每季度学习一门新技术/语言拓宽视野。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='全栈工程师' AND user_id=1),
 'TECHNICAL', 'Node.js 异步编程模型',
 '请详细说明 Node.js 的事件循环机制、libuv 线程池，以及如何避免阻塞事件循环。',
 '事件循环阶段（按顺序）：timers → pending callbacks → idle/prepare → poll（轮询 I/O）→ check（setImmediate）→ close callbacks。宏任务（setTimeout/setInterval/I/O）和微任务（Promise.then/queueMicrotask）的区别：每个宏任务后清空微任务队列。libuv 线程池（默认 4 线程）：处理 fs/crypto/dns/zlib 等 CPU 密集/阻塞型 API。避免阻塞：1) 不在主线程做大量计算（用 Worker Threads）；2) 合理设置 UV_THREADPOOLSIZE；3) 流式处理大文件（stream 而非 readFile）；4) 使用集群模式（cluster）利用多核。',
 'SENIOR'),

(1, (SELECT id FROM job_position WHERE title='全栈工程师' AND user_id=1),
 'SCENARIO', '数据库选型：关系型 vs NoSQL',
 '一个社交 App 需要存储用户信息、动态（Feed）、点赞关系、聊天消息，你会如何选择和组合数据库？',
 '混合方案（Polyglot Persistence）：1) 用户信息 → PostgreSQL（结构化数据、ACID 事务、复杂查询、JSONB 字段存扩展属性）；2) Feed 动态 → PostgreSQL（时间线查询 + JOIN 用户信息）或 Cassandra（写入量大时）；3) 点赞关系 → Redis Set（O(1) 点赞/取消 + SINTER 求交集推荐）；4) 聊天消息 → MongoDB（文档模型天然契合消息结构 + 时间范围查询）或 TimescaleDB（时序扩展 SQL）；5) 全文搜索 → Elasticsearch（动态内容检索）。核心原则：没有银弹，按数据特性选型。',
 'SENIOR'),

(1, (SELECT id FROM job_position WHERE title='全栈工程师' AND user_id=1),
 'TECHNICAL', '前端状态管理方案对比',
 '请对比 Redux/Zustand/Pinia/Jotai 等状态管理方案的特点和适用场景。',
 'Redux：单一 Store + 纯函数 Reducer + 中间件（Thunk/Saga），适合大型复杂应用，但样板代码多。Zustand：轻量（~1KB）、API 简洁（setState 直接修改）、无 Provider 包裹、支持 middleware 和 slice，中小型项目首选。Pinia：Vue 3 官方推荐、去 mutations（直接修改 state）、TypeScript 友好、DevTools 支持。Jotai：原子化（Atomic）状态管理、细粒度渲染（只重渲染依赖的 atom）、适合高度组件化的场景。选择依据：团队规模 + 应用复杂度 + 性能需求。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='全栈工程师' AND user_id=1),
 'SCENARIO', '国际化（i18n）方案设计与实施',
 '产品要支持中英日三种语言的国际化，包括 UI 文本、日期格式、数字格式、RTL 布局，你会如何设计 i18n 方案？',
 '实施方案：1) 框架选型：react-i18next / vue-i18n（均支持 interpolation/plural/namespace）；2) 文本管理：JSON 语言包按功能模块拆分（common.json / home.json / settings.json），提取硬编码字符串用 t("key") 替代；3) 日期/数字格式：Intl.DateTimeFormat / Intl.NumberFormat（自动适配 locale）；4) RTL 支持：CSS logical properties（margin-inline-start 替代 margin-left）、dir="rtl" 属性 + start/end 语义；5) 流程：翻译管理平台（Crowdin/Phrase/Locize）→ CI 自动提取新增 key → 翻译回写 → 构建；6) 图片/图标：locale-aware（不同文化背景的插图差异）。',
 'MEDIUM'),

(1, (SELECT id FROM job_position WHERE title='全栈工程师' AND user_id=1),
 'BEHAVIORAL', '独立负责一个完整项目',
 '请描述一次你独立负责一个完整项目（从需求到上线运维）的经历，你遇到的最大挑战是什么？',
 'STAR 作答：项目背景和目标（情境）、你的职责范围（任务）、技术选型/架构设计/开发推进/上线部署过程中遇到的具体困难（如需求变更/技术债务/性能瓶颈）及解决过程（行动）、最终上线结果和用户反馈/数据表现（结果）。重点突出：全栈能力（前后端/数据库/部署都能 cover）、项目管理能力（进度把控/风险预判/沟通协调）、以及从中学到的经验教训。',
 'MEDIUM');


-- ============ 四、为所有题目建立标签关联 ============
-- 注意：题目 ID 从 15 开始（原有 14 道 ID 1-14）
-- 使用子查询匹配标题来关联标签

-- ===== 原 V19 的 14 道题（ID 1-14）的标签 =====

-- Q1: HashMap 底层实现 → Java, 并发编程
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
WHERE q.title = 'HashMap 的底层实现原理' AND t.name IN ('Java', '并发编程');

-- Q2: Spring 事务失效 → Spring, Java
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
WHERE q.title = 'Spring 事务失效的常见场景' AND t.name IN ('Spring', 'Java');

-- Q3: 短链接系统 → 系统设计, Java
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
WHERE q.title = '设计一个短链接系统' AND t.name IN ('系统设计', 'Java');

-- Q4: 线上故障排查 → 沟通表达, Java
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
WHERE q.title = '一次线上故障的排查经历' AND t.name IN ('沟通表达', 'Java');

-- Q5: URL到页面展示 → JavaScript, 网络协议
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
WHERE q.title = '浏览器从输入 URL 到页面展示的过程' AND t.name IN ('JavaScript', '网络协议');

-- Q6: JS事件循环 → JavaScript, 前端框架
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
WHERE q.title = 'JavaScript 事件循环机制' AND t.name IN ('JavaScript', '前端框架');

-- Q7: 首屏加载优化 → 前端框架, JavaScript
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
WHERE q.title = '首屏加载优化方案' AND t.name IN ('前端框架', 'JavaScript');

-- Q8: 与后端接口分歧 → 沟通表达, 团队协作
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
WHERE q.title = '与后端就接口设计产生分歧' AND t.name IN ('沟通表达', '团队协作');

-- Q9: 老年人打车App → 产品思维, 用户体验
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
WHERE q.title = '如何设计一款老年人使用的打车 App' AND t.name IN ('产品思维', '用户体验');

-- Q10: 数据指标体系 → 数据分析, 产品思维
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
WHERE q.title = '如何搭建产品的数据指标体系' AND t.name IN ('数据分析', '产品思维');

-- Q11: 跨部门项目落地 → 团队协作, 项目管埋
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
WHERE q.title = '推动一个跨部门项目落地' AND t.name IN ('团队协作', '项目管理');

-- Q12: SQL薪资第二高 → 数据分析, 数据库
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
WHERE q.title = 'SQL 求每个部门薪资第二高的员工' AND t.name IN ('数据分析', '数据库');

-- Q13: DAU突降分析 → 数据分析
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
WHERE q.title = '某产品 DAU 突然下降 20%' AND t.name = '数据分析';

-- Q14: 数据驱动决策 → 数据分析, 沟通表达
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
WHERE q.title = '用数据分析驱动一次业务决策' AND t.name IN ('数据分析', '沟通表达');


-- ===== V24 新增题目的标签（按岗位批量关联） =====

-- Java 后端新增 10 题 → Java, Spring, JVM, Redis, 微服务, 消息队列, 数据库, 系统设计, 并发编程, 学习能力
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
JOIN job_position j ON q.job_id = j.id
WHERE j.title = 'Java 后端开发工程师' AND q.id > 14
AND t.name IN ('Java','Spring','JVM','Redis','微服务','消息队列','数据库','系统设计','并发编程','学习能力');

-- 前端新增 10 题 → JavaScript, 前端框架, 网络协议, 安全, 算法, 学习能力
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
JOIN job_position j ON q.job_id = j.id
WHERE j.title = '前端开发工程师' AND q.id > 14
AND t.name IN ('JavaScript','前端框架','网络协议','安全','算法','学习能力');

-- 产品经理新增 8 题 → 产品思维, 用户体验, 需求分析, 原型设计, 数据分析, 沟通表达, 项目管埋
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
JOIN job_position j ON q.job_id = j.id
WHERE j.title = '产品经理' AND q.id > 14
AND t.name IN ('产品思维','用户体验','需求分析','原型设计','数据分析','沟通表达','项目管理');

-- 数据分析师新增 8 题 → 数据分析, Python, 数据可视化, 机器学习, SQL, 学习能力, 沟通表达
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
JOIN job_position j ON q.job_id = j.id
WHERE j.title = '数据分析师' AND q.id > 14
AND t.name IN ('数据分析','Python','数据可视化','机器学习','SQL','学习能力','沟通表达');

-- 测试开发新增 10 题 → 测试, 自动化, 性能测试, CI/CD, Python, 安全, 沟通表达
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
JOIN job_position j ON q.job_id = j.id
WHERE j.title = '测试开发工程师' AND q.id > 14
AND t.name IN ('测试','自动化','性能测试','CI/CD','Python','安全','沟通表达');

-- 运维新增 10 题 → Docker, Kubernetes, Linux, CI/CD, 监控, 网络协议, Shell(用Linux代替), 安全
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
JOIN job_position j ON q.job_id = j.id
WHERE j.title = '运维工程师' AND q.id > 14
AND t.name IN ('Docker','Kubernetes','Linux','CI/CD','监控','网络协议','安全');

-- UI/UX设计师新增 8 题 → 设计规范, 用户体验, Figma, 产品思维, 沟通表达
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
JOIN job_position j ON q.job_id = j.id
WHERE j.title = 'UI/UX 设计师' AND q.id > 14
AND t.name IN ('设计规范','用户体验','Figma','产品思维','沟通表达');

-- 全栈工程师新增 10 题 → Node.js, JavaScript, 前端框架, 数据库, Redis, 微服务, 网络协议, 安全
INSERT INTO question_tag_rel(question_id, tag_id, user_id)
SELECT q.id, t.id, 1
FROM interview_question q JOIN question_tag t ON t.user_id = 1
JOIN job_position j ON q.job_id = j.id
WHERE j.title = '全栈工程师' AND q.id > 14
AND t.name IN ('Node.js','JavaScript','前端框架','数据库','Redis','微服务','网络协议','安全');
