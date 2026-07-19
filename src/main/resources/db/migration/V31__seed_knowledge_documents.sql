-- V23: 种子知识文档 —— 替换笼统占位数据为具体可用的面试参考资料
-- 每条文档附带 2-3 个内容片段（document_chunk），状态 READY
-- 使用显式 ID，避免依赖 last_insert_rowid() 在多行 INSERT 中的不确定性

-- 先清理旧的笼统占位数据
DELETE FROM document_chunk WHERE document_id IN (SELECT id FROM document WHERE name = '后续计划');
DELETE FROM document WHERE name = '后续计划';

-- 1. Java 核心原理与并发编程 (id=1)
INSERT INTO document(id, user_id, name, original_name, storage_name, storage_path,
                     file_type, file_size, category, status, create_time, update_time, deleted)
VALUES(1, 1, 'Java 核心原理与并发编程', 'java_core_concurrency.md',
       'seed_java_core.md', '', 'md', 0,
       '技术资料', 'READY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO document_chunk(id, user_id, document_id, chunk_index, content, token_count,
                          vector_id, metadata_json, create_time, deleted)
VALUES
  (1, 1, 1, 0,
   '## JVM 内存模型\n\nJVM 运行时数据区包括：程序计数器、虚拟机栈、本地方法栈、堆、方法区。\n- **堆**：所有线程共享，存放对象实例，是 GC 主要区域。\n- **方法区**：存储类信息、常量池、静态变量。JDK8 后用元空间(Metaspace)替代永久代。\n- **虚拟机栈**：每个线程私有，存放栈帧（局部变量表、操作数栈、动态链接、返回地址）。\n- **垃圾回收**：主要算法包括标记-清除、复制、标记-整理。分代收集将堆分为新生代（Eden+S0+S1）和老年代。',
   180, NULL, '{}', CURRENT_TIMESTAMP, 0),
  (2, 1, 1, 1,
   '## 并发编程基础\n\n### 线程创建方式\n1. 继承 Thread 类\n2. 实现 Runnable 接口\n3. 实现 Callable 接口 + FutureTask\n4. 线程池（推荐）\n\n### synchronized vs ReentrantLock\n| 特性 | synchronized | ReentrantLock |\n|------|-------------|---------------|\n| 实现 | JVM 层面 | API 层面 |\n| 锁释放 | 自动 | 手动 unlock() |\n| 公平锁 | 非公平 | 可选公平/非公平 |\n| 条件变量 | 单一 Condition | 多个 Condition |\n\n### volatile 关键字\n保证可见性和有序性，不保证原子性。适用于：状态标志位、一次性安全发布（双重检查锁定）。',
   195, NULL, '{}', CURRENT_TIMESTAMP, 0),
  (3, 1, 1, 2,
   '## 线程池与实战\n\nThreadPoolExecutor 七参数：\n- corePoolSize / maximumPoolSize：核心/最大线程数\n- keepAliveTime / unit：空闲线程存活时间\n- workQueue：任务队列（ArrayBlockingQueue / LinkedBlockingQueue / SynchronousQueue）\n- threadFactory：线程工厂\n- handler：拒绝策略（AbortPolicy / CallerRunsPolicy / DiscardPolicy / DiscardOldestPolicy）\n\n**面试高频问题**：\n- 如何合理设置线程池参数？→ 根据 CPU 密集型（N+1）或 IO 密集型（2N）调整。\n- 线程池如何优雅关闭？→ shutdown() + awaitTermination()。\n- ThreadPoolExecutor 与 Executors 工厂方法的区别？→ 工厂方法队列无界可能导致 OOM。',
   178, NULL, '{}', CURRENT_TIMESTAMP, 0);


-- 2. 系统设计面试指南 (id=2)
INSERT INTO document(id, user_id, name, original_name, storage_name, storage_path,
                     file_type, file_size, category, status, create_time, update_time, deleted)
VALUES(2, 1, '系统设计面试指南', 'system_design_guide.md',
       'seed_system_design.md', '', 'md', 0,
       '面试指南', 'READY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO document_chunk(id, user_id, document_id, chunk_index, content, token_count,
                          vector_id, metadata_json, create_time, deleted)
VALUES
  (4, 1, 2, 0,
   '## 系统设计方法论\n\n### 设计流程（4S 法则）\n1. **Scenario（场景）**：明确功能需求和非功能需求（QPS、延迟、一致性要求）。\n2. **Service（服务）**：粗粒度拆分核心服务，画出高层架构图。\n3. **Scale（扩展）**：识别瓶颈点——数据库？缓存？消息队列？逐层优化。\n4. **Safety（容灾）**：单点故障消除、降级熔断、多活部署。\n\n### 常见设计模式\n- **负载均衡**：L4（IP/端口）、L7（HTTP 头部）；策略包括轮询、加权、最少连接、一致性哈希。\n- **缓存策略**：Cache-Aside（旁路缓存）、Read-Through、Write-Through、Write-Behind；缓存穿透/击穿/雪崩的解决方案。\n- **数据库分片**：水平拆分（范围/哈希）、垂直拆分；读写分离；分布式事务（2PC/TCC/Saga）。',
   198, NULL, '{}', CURRENT_TIMESTAMP, 0),
  (5, 1, 2, 1,
   '## 经典系统设计题目解析\n\n### URL 短链服务\n- 生成策略：Base62 编号（10数字+26大写+26小写=62字符），ID 自增或哈希去重。\n- 存储：Redis 存热点映射 + MySQL 持久化。\n- 扩展：布隆过滤器防重复写入。\n\n### 分布式消息队列\n- 角色：生产者、Broker、消费者、注册中心。\n- 可靠性：ACK 机制、重试队列、死信队列、事务消息。\n- 顺序消费：分区键 + 单消费者；幂等处理用唯一 ID 去重。\n\n### 分布式限流\n- 算法：令牌桶（允许突发）、漏桶（恒定速率）、滑动窗口（更精确）。\n- 层级：网关限流 → 服务限流 → DB 连接池限制。',
   186, NULL, '{}', CURRENT_TIMESTAMP, 0),
  (6, 1, 2, 2,
   '## 估算技巧（Fermi 问题）\n\n面试官考察的是思路而非精确答案：\n1. **明确假设**：先问清边界条件（日活？峰值倍率？读写比例？）。\n2. **自顶向下分解**：总请求 = 用户数 × 人均请求 × 峰值系数。\n3. **存储估算**：每条记录大小 × QPS × 保留天数 × 冗余系数。\n\n**示例 — 设计 Instagram 存储**：\n- 日活 5 亿，人均发 1.5 张图 → 日增 7.5 亿张\n- 平均图片 2MB → 日增量约 1.5PB\n- 三副本冗余 → 4.5PB/天\n- 冷热分离：30天热存 SSD + 归档对象存储',
   145, NULL, '{}', CURRENT_TIMESTAMP, 0);


-- 3. MySQL 高性能优化 (id=3)
INSERT INTO document(id, user_id, name, original_name, storage_name, storage_path,
                     file_type, file_size, category, status, create_time, update_time, deleted)
VALUES(3, 1, 'MySQL 高性能优化实战', 'mysql_optimization.md',
       'seed_mysql_perf.md', '', 'md', 0,
       '技术资料', 'READY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO document_chunk(id, user_id, document_id, chunk_index, content, token_count,
                          vector_id, metadata_json, create_time, deleted)
VALUES
  (7, 1, 3, 0,
   '## 索引优化\n\n### 索引类型\n- **B+Tree 聚簇索引**：InnoDB 主键索引，叶子节点存完整行数据。\n- **二级索引**：叶子节点存主键值，回表查询。\n- **联合索引**：最左前缀匹配原则；覆盖索引避免回表。\n\n### 索引失效场景\n1. 对索引列使用函数（如 WHERE YEAR(create_time) = 2024）。\n2. LIKE 以通配符开头（LIKE ''%keyword''）。\n3. 隐式类型转换（字符串列传数字比较）。\n4. OR 条件中有一个字段没索引。\n5. 联合索引跳过中间列。\n\n### EXPLAIN 重点字段\n- **type**：const > eq_ref > ref > range > index > ALL（目标 ≥ range）。\n- **Extra**：Using filesort / Using temporary 需要优化；Using index 说明覆盖索引命中。',
   192, NULL, '{}', CURRENT_TIMESTAMP, 0),
  (8, 1, 3, 1,
   '## SQL 调优实战\n\n### 分页优化\n传统 LIMIT offset,N 在深度分页时性能差：\n```sql\n-- 优化方案：延迟关联\nSELECT * FROM t1 INNER JOIN (\n    SELECT id FROM t1 ORDER BY id LIMIT 100000, 20\n) AS t2 USING(id);\n```\n\n### 慢查询分析流程\n1. 开启慢查询日志（long_query_time = 1s）。\n2. 用 mysqldumpsort 分析汇总。\n3. EXPLAIN 定位执行计划瓶颈。\n4. 用 SHOW PROFILE 分析执行阶段耗时。\n5. 优化后用 pt-query-digest 持续监控。\n\n### 锁机制\n- 共享锁(S)：SELECT ... LOCK IN SHARE MODE\n- 排他锁(X)：SELECT ... FOR UPDATE\n- 间隙锁(Gap Lock)：防止幻读，RR 隔离级别下范围查询产生。',
   175, NULL, '{}', CURRENT_TIMESTAMP, 0);


-- 4. 行为面试 STAR 方法 (id=4)
INSERT INTO document(id, user_id, name, original_name, storage_name, storage_path,
                     file_type, file_size, category, status, create_time, update_time, deleted)
VALUES(4, 1, '行为面试 STAR 方法与高分回答模板', 'behavioral_interview_star.md',
       'seed_behavioral_star.md', '', 'md', 0,
       '面试指南', 'READY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO document_chunk(id, user_id, document_id, chunk_index, content, token_count,
                          vector_id, metadata_json, create_time, deleted)
VALUES
  (9, 1, 4, 0,
   '## STAR 方法详解\n\n行为面试(Behavioral Interview)通过你过去的经历预测未来表现。STAR 是回答框架：\n\n### S - Situation（情境）\n简明交代背景：什么项目？什么角色？时间压力/资源约束？\n> "去年双十一大促期间，我负责订单系统的稳定性保障..."\n\n### T - Task（任务）\n你的职责和目标是什么？\n> "...目标是确保 QPS 从 5 万提升到 20 万且 P99 延迟控制在 200ms 以内..."\n\n### A - Action（行动）\n**这是最关键的部分！** 具体做了什么决策、用了什么技术、怎么协作？\n> "我做了三件事：(1) 引入 Redis 多级缓存降低 DB 压力 70%；(2) 将同步下单改为异步消息队列削峰；(3) 设计了自动降级预案当 Redis 故障时切到直连 DB..."\n\n### R - Result（结果）\n量化成果：提升了多少？节省了多少？获得了什么认可？\n> "...最终大促当天峰值 QPS 达到 23 万，P99 延迟 156ms，零故障。团队因此获得年度技术突破奖。"',
   218, NULL, '{}', CURRENT_TIMESTAMP, 0),
  (10, 1, 4, 1,
   '## 高频行为题与回答要点\n\n### 1. "说说你遇到的最大挑战"\n**错误回答**："加班太多"（抱怨型）、"没有挑战"（缺乏反思）\n**高分结构**：选择一个有技术含量且有成长的故事，突出解决问题的过程。\n\n### 2. "描述一次与团队的冲突"\n**重点不是冲突本身，而是你怎么处理的**：\n- 先倾听对方观点\n- 用数据和事实沟通\n- 寻找共同目标\n- 最终达成一致或找到折中方案\n\n### 3. "为什么离开上一家公司"\n聚焦于**追求更大的成长空间/技术挑战**，不要说前任公司坏话。\n\n### 回答通用原则\n- 准备 5-8 个不同维度的故事（技术攻坚、跨团队协作、领导力、失败复盘）。\n- 每个故事控制在 2 分钟内讲完。\n- 用数字说话（提升 X%、节省 Y 小时、支撑 Z 用户）。\n- 体现学习能力和自我驱动力。',
   196, NULL, '{}', CURRENT_TIMESTAMP, 0);


-- 5. Spring Boot 微服务架构 (id=5)
INSERT INTO document(id, user_id, name, original_name, storage_name, storage_path,
                     file_type, file_size, category, status, create_time, update_time, deleted)
VALUES(5, 1, 'Spring Boot 微服务架构设计', 'springboot_microservices.md',
       'seed_springboot_microservice.md', '', 'md', 0,
       '架构设计', 'READY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO document_chunk(id, user_id, document_id, chunk_index, content, token_count,
                          vector_id, metadata_json, create_time, deleted)
VALUES
  (11, 1, 5, 0,
   '## 微服务核心概念\n\n### 单体 vs 微服务的权衡\n| 维度 | 单体应用 | 微服务 |\n|------|---------|--------|\n| 开发初期效率 | 快 | 较慢 |\n| 部署复杂度 | 低 | 高（需 CI/CD + 容器编排） |\n| 技术异构 | 受限 | 每服务可选最适合的技术栈 |\n| 故障隔离 | 一个 bug 全挂 | 单个服务故障不影响全局 |\n| 数据一致性 | 本地事务简单 | 需要分布式事务方案 |\n\n### Spring Cloud 核心组件\n- **Nacos/Eureka**：服务注册发现 + 配置中心\n- **Sentinel**：流量控制、熔断降级\n- **OpenFeign**：声明式 HTTP 客户端（负载均衡 + 重试）\n- **Gateway**：API 网关（路由、鉴权、限流）\n- **Seata**：分布式事务（AT/TCC/SAGA 模式）\n- **Sleuth + Zipkin**：链路追踪',
   188, NULL, '{}', CURRENT_TIMESTAMP, 0),
  (12, 1, 5, 1,
   '## 分布式常见问题\n\n### 服务间通信\n- **同步**：REST / gRPC（强一致性场景）\n- **异步**：RabbitMQ / Kafka / RocketMQ（解耦、削峰、最终一致性）\n\n### 分布式事务方案对比\n| 方案 | 一致性 | 性能 | 复杂度 | 适用场景 |\n|------|--------|------|--------|----------|\n| 2PC | 强 | 低 | 中 | 传统金融核心 |\n| TCC(Try-Confirm-Cancel) | 强 | 中 | 高 | 支付、库存扣减 |\n| Saga（长事务） | 最终 | 高 | 中 | 跨系统长流程 |\n| 本地消息表 | 最终 | 高 | 低 | 异步通知类场景 |\n\n### 幂等性设计\n接口必须保证幂等：\n1. 唯一约束（数据库层面）\n2. 乐观锁（版本号 CAS）\n3. 分布式锁（Redis setnx / Redisson）\n4. Token 令牌（提交时销毁防重复）',
   182, NULL, '{}', CURRENT_TIMESTAMP, 0);


-- 6. 算法与数据结构精讲 (id=6)
INSERT INTO document(id, user_id, name, original_name, storage_name, storage_path,
                     file_type, file_size, category, status, create_time, update_time, deleted)
VALUES(6, 1, '算法与数据结构面试精讲', 'algorithm_datastructure.md',
       'seed_algorithm_ds.md', '', 'md', 0,
       '技术资料', 'READY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO document_chunk(id, user_id, document_id, chunk_index, content, token_count,
                          vector_id, metadata_json, create_time, deleted)
VALUES
  (13, 1, 6, 0,
   '## 必掌握的数据结构\n\n### 数组 & 链表\n- 数组：O(1) 随机访问，O(n) 插入删除；适合读多写少。\n- 链表：O(n) 查找，O(1) 头尾操作；反转/环检测/合并是高频题。\n\n### 栈 & 队列\n- 栈：括号匹配、单调栈（下一个更大元素）、DFS 递归本质就是栈。\n- 队列：BFS 层序遍历、滑动窗口最大值（单调队列）。\n\n### 哈希表\n- O(1) 平均查找插入；处理冲突：拉链法 / 开放寻址。\n- 经典题：两数之和、字母异位词分组、LRU 缓存（HashMap + 双向链表）。\n\n### 树\n- 二叉树遍历：前/中/后/层序递归与非递归写法都要会。\n- BST：左 < 根 < 右；查找/插入 O(log n)。\n- AVL/红黑树：自平衡二叉搜索树；TreeMap/TreeSet 底层实现。\n- 堆：优先队列；Top K 问题、归并排序（小根堆归并 K 个有序数组）。',
   210, NULL, '{}', CURRENT_TIMESTAMP, 0),
  (14, 1, 6, 1,
   '## 必刷算法题型\n\n### Top 10 高频算法模式\n1. **双指针**：三数之和（排序+双指针）、容器盛最多水、移除元素。\n2. **滑动窗口**：最长无重复子串、最小覆盖子串、定长窗口求和。\n3. **二分查找**：搜索旋转排序数组、在排序矩阵中查找目标值。\n4. **BFS/DFS**：岛屿数量、单词搜索、图的拓扑排序。\n5. **回溯**：全排列、组合总和、N 皇后。\n6. **动态规划**：爬楼梯（入门）、最长公共子序列、背包问题、编辑距离。\n7. **贪心**：跳跃游戏、区间调度（按结束时间排序）。\n8. **并查集**：连通分量、最小生成树(Kruskal)。\n9. **前缀树(Trie)**：单词搜索 II、自动补全。\n10. **线段树**：区间更新与查询（进阶题）。\n\n### 刷题建议\n- LeetCode Hot 100 → 精选 75 → 按专题深入。\n- 每道题追求多种解法并比较时空复杂度。\n- 面试时先说思路再写代码，注意边界条件和输入校验。',
   205, NULL, '{}', CURRENT_TIMESTAMP, 0);
