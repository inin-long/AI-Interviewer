-- 岗位库与面试题库种子数据（绑定单机默认用户 user_id = 1）
-- 提供开箱即用的岗位、面试题与标签，避免新装应用题库为空。
-- 全新安装时自动创建默认开发用户（INSERT OR IGNORE 保证幂等）。

-- ============ 默认开发用户 ============
INSERT OR IGNORE INTO user(id, username, password_hash, nickname) VALUES
(1, 'dev', '', '开发者');

-- ============ 岗位 ============
INSERT INTO job_position(user_id, title, department, description) VALUES
(1, 'Java 后端开发工程师', '研发中心', '负责后端服务设计与开发，熟悉 Spring 生态、数据库与分布式系统，具备高并发与性能优化经验。'),
(1, '前端开发工程师', '研发中心', '负责 Web 前端开发，精通 HTML/CSS/JavaScript 与主流框架，注重工程化、性能与用户体验。'),
(1, '产品经理', '产品中心', '负责产品规划与需求管理，具备用户洞察、竞品分析、数据驱动决策与跨团队协作能力。'),
(1, '数据分析师', '数据中心', '负责业务数据分析与指标体系建设，熟悉 SQL、统计方法与可视化，能从数据中发现业务机会。');

-- ============ 标签 ============
INSERT INTO question_tag(user_id, name) VALUES
(1, 'Java'), (1, 'Spring'), (1, '并发编程'), (1, '数据库'), (1, '系统设计'),
(1, 'JavaScript'), (1, '前端框架'), (1, '算法'), (1, '沟通表达'), (1, '团队协作'),
(1, '产品思维'), (1, '数据分析');

-- ============ 面试题：Java 后端 ============
INSERT INTO interview_question(user_id, job_id, category, title, content, reference_answer, difficulty) VALUES
(1, (SELECT id FROM job_position WHERE title='Java 后端开发工程师' AND user_id=1),
 'TECHNICAL', 'HashMap 的底层实现原理',
 '请说明 Java 中 HashMap 的底层数据结构、扩容机制，以及 JDK 8 相比之前版本的改进。',
 '数组 + 链表 + 红黑树。默认容量 16、负载因子 0.75，超过阈值扩容为 2 倍。JDK 8 中当链表长度 ≥ 8 且数组长度 ≥ 64 时链表转红黑树，查询由 O(n) 降为 O(log n)；并优化了扩容时的 rehash（高低位拆分，无需重新计算 hash）。',
 'MEDIUM'),
(1, (SELECT id FROM job_position WHERE title='Java 后端开发工程师' AND user_id=1),
 'TECHNICAL', 'Spring 事务失效的常见场景',
 '@Transactional 在哪些情况下会失效？如何避免？',
 '常见失效场景：1) 方法非 public；2) 同类内部方法自调用（未走代理）；3) 异常被 catch 未抛出；4) 抛出的是受检异常而 rollbackFor 未配置；5) 数据库引擎不支持事务（如 MyISAM）；6) 未被 Spring 管理的对象调用。避免：通过代理对象调用、正确配置 rollbackFor、确保异常向上传播。',
 'HARD'),
(1, (SELECT id FROM job_position WHERE title='Java 后端开发工程师' AND user_id=1),
 'SCENARIO', '设计一个短链接系统',
 '如果让你设计一个类似 bit.ly 的短链接服务，日均生成 1000 万条短链，你会如何设计？',
 '要点：1) 发号器（雪花算法/号段模式）生成唯一 ID，再 Base62 编码为短码；2) 存储用 KV（Redis）+ 持久化（MySQL）；3) 读多写少，用缓存 + CDN；4) 302 重定向 vs 301 的权衡；5) 防刷、过期策略、统计埋点。考察容量估算与权衡取舍。',
 'HARD'),
(1, (SELECT id FROM job_position WHERE title='Java 后端开发工程师' AND user_id=1),
 'BEHAVIORAL', '一次线上故障的排查经历',
 '请描述一次你负责排查并解决线上故障的经历，你是如何定位问题的？',
 '用 STAR 法则作答：说明故障现象（情境）、你的职责（任务）、排查手段如日志/监控/链路追踪/复现（行动）、最终定位根因与修复+复盘改进（结果）。重点体现系统化排查思路与责任心。',
 'MEDIUM');

-- ============ 面试题：前端 ============
INSERT INTO interview_question(user_id, job_id, category, title, content, reference_answer, difficulty) VALUES
(1, (SELECT id FROM job_position WHERE title='前端开发工程师' AND user_id=1),
 'TECHNICAL', '浏览器从输入 URL 到页面展示的过程',
 '请描述在浏览器地址栏输入 URL 后，到页面完整展示，中间发生了什么？',
 'DNS 解析 → 建立 TCP 连接（TLS 握手）→ 发送 HTTP 请求 → 服务器响应 → 浏览器解析 HTML 构建 DOM、解析 CSS 构建 CSSOM → 合成渲染树 → 布局(Layout) → 绘制(Paint) → 合成(Composite)。可延伸讲重排重绘、资源加载优化、缓存策略。',
 'MEDIUM'),
(1, (SELECT id FROM job_position WHERE title='前端开发工程师' AND user_id=1),
 'TECHNICAL', 'JavaScript 事件循环机制',
 '请解释 JS 的 Event Loop，以及宏任务与微任务的执行顺序。',
 '主线程执行同步代码 → 清空微任务队列（Promise.then、queueMicrotask、MutationObserver）→ 执行一个宏任务（setTimeout、setInterval、I/O）→ 再清空微任务 → 循环。每轮宏任务后都会清空所有微任务。',
 'MEDIUM'),
(1, (SELECT id FROM job_position WHERE title='前端开发工程师' AND user_id=1),
 'SCENARIO', '首屏加载优化方案',
 '一个页面首屏加载很慢，用户反馈明显卡顿，你会从哪些方面优化？',
 '网络层：CDN、HTTP2、资源压缩、缓存；资源层：代码分割、懒加载、Tree Shaking、图片优化(WebP/懒加载)；渲染层：SSR/预渲染、骨架屏、关键 CSS 内联；监控：用 Lighthouse/Performance API 定位瓶颈，量化 FCP/LCP 指标。',
 'HARD'),
(1, (SELECT id FROM job_position WHERE title='前端开发工程师' AND user_id=1),
 'BEHAVIORAL', '与后端就接口设计产生分歧',
 '当你和后端同学对接口设计有不同意见时，你是怎么沟通和推进的？',
 'STAR 作答：说明分歧点（情境）、目标（任务）、如何用数据/规范/用户体验为依据沟通，寻求折中或引入第三方评审（行动）、达成一致并沉淀规范（结果）。体现协作与解决问题的能力。',
 'EASY');

-- ============ 面试题：产品经理 ============
INSERT INTO interview_question(user_id, job_id, category, title, content, reference_answer, difficulty) VALUES
(1, (SELECT id FROM job_position WHERE title='产品经理' AND user_id=1),
 'SCENARIO', '如何设计一款老年人使用的打车 App',
 '请为老年人群体设计一款打车 App，你会重点考虑哪些方面？',
 '用户洞察：老年人视力、操作习惯、信任感。设计要点：大字体大按钮、语音输入、一键呼叫、常用地址、亲情号代付、简化流程、线下引导。用需求优先级排序，说明如何验证（用户访谈/小范围灰度）。考察同理心与结构化思维。',
 'MEDIUM'),
(1, (SELECT id FROM job_position WHERE title='产品经理' AND user_id=1),
 'TECHNICAL', '如何搭建产品的数据指标体系',
 '请说明你会如何为一个新产品搭建核心指标体系（如 AARRR）。',
 'AARRR 模型：获取(Acquisition)、激活(Activation)、留存(Retention)、收入(Revenue)、传播(Referral)。为每个环节定义北极星指标与过程指标，区分虚荣指标与可行动指标，建立漏斗监控并结合业务阶段调整重点。',
 'MEDIUM'),
(1, (SELECT id FROM job_position WHERE title='产品经理' AND user_id=1),
 'BEHAVIORAL', '推动一个跨部门项目落地',
 '描述一次你推动跨部门协作、最终把项目落地的经历。',
 'STAR 作答：项目背景与阻力（情境）、你的角色（任务）、如何对齐目标、协调资源、管理进度与冲突（行动）、交付结果与复盘（结果）。体现推动力与影响力。',
 'MEDIUM');

-- ============ 面试题：数据分析师 ============
INSERT INTO interview_question(user_id, job_id, category, title, content, reference_answer, difficulty) VALUES
(1, (SELECT id FROM job_position WHERE title='数据分析师' AND user_id=1),
 'TECHNICAL', 'SQL 求每个部门薪资第二高的员工',
 '给定 employee(id, name, dept_id, salary) 表，请写 SQL 查询每个部门薪资第二高的员工。',
 '可用窗口函数：SELECT * FROM (SELECT *, DENSE_RANK() OVER(PARTITION BY dept_id ORDER BY salary DESC) rk FROM employee) t WHERE rk = 2; 考察窗口函数与分组排名，注意并列处理（DENSE_RANK vs RANK vs ROW_NUMBER 的区别）。',
 'MEDIUM'),
(1, (SELECT id FROM job_position WHERE title='数据分析师' AND user_id=1),
 'SCENARIO', '某产品 DAU 突然下降 20%',
 '如果你发现某产品的日活(DAU)昨天突然下降了 20%，你会如何分析原因？',
 '结构化拆解：1) 确认数据准确性（埋点/统计口径是否异常）；2) 拆维度：渠道、地区、设备、版本、新老用户；3) 拆环节：是否某功能/页面异常；4) 结合外部因素（节假日、竞品、大盘、事故）；5) 定位后验证并给出建议。体现 MECE 与假设验证思维。',
 'HARD'),
(1, (SELECT id FROM job_position WHERE title='数据分析师' AND user_id=1),
 'BEHAVIORAL', '用数据分析驱动一次业务决策',
 '请举例说明你如何通过一次数据分析，帮助业务做出了决策或带来改进。',
 'STAR 作答：业务问题（情境）、分析目标（任务）、数据获取与分析方法、如何得出洞察并说服相关方（行动）、决策落地后的业务效果（结果，尽量量化）。体现数据价值转化能力。',
 'MEDIUM');
