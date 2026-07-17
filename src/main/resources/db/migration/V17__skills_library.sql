-- 面试技巧库：分类资料（STAR 法则、礼仪指南等）
CREATE TABLE skill_article (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id          INTEGER,
    category         VARCHAR(32) NOT NULL DEFAULT 'GENERAL'
                                      CHECK (category IN ('STAR', 'ETIQUETTE', 'GENERAL', 'BEHAVIOR')),
    title            VARCHAR(255) NOT NULL,
    summary          VARCHAR(512),
    content_markdown TEXT         NOT NULL,
    tags_json        TEXT         NOT NULL DEFAULT '[]',
    create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER      NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE INDEX idx_skill_article_user ON skill_article(user_id, deleted);
CREATE INDEX idx_skill_article_category ON skill_article(category, deleted);

-- 全局种子资料（user_id 为 NULL 表示面向所有用户的公共资料）
INSERT INTO skill_article(id, user_id, category, title, summary, content_markdown, tags_json) VALUES
(1, NULL, 'STAR', 'STAR 法则：用结构化故事回答行为题',
 'Situation / Task / Action / Result 四段式，让回答有情境、有动作、有结果。',
 '# STAR 法则\n\n行为类面试题（"讲一次你解决冲突的经历"）最怕泛泛而谈。STAR 帮你把经历讲成有说服力的故事。\n\n## S — Situation 情境\n用一两句话交代背景：什么项目、什么团队、遇到了什么约束。避免堆砌无关细节。\n\n## T — Task 任务\n你在其中承担的角色与目标是什么？一句话说清"你要解决什么"。\n\n## A — Action 行动\n这是重点，占回答 60% 以上。用"我"开头的具体动作：你做了什么决策、调用了哪些资源、如何推动。\n\n## R — Result 结果\n用可量化结果收尾：指标提升多少、节省多少成本、获得了什么反馈。即使失败，也要说明你学到了什么。\n\n## 常见误区\n- 只讲团队成果，没有"我"的贡献\n- Action 太空，缺少具体动作\n- 没有 Result，故事没有落点\n\n> 练习：选一段你最得意的经历，按四段各写两句话，控制在 2 分钟内讲完。',
 '["行为面试","回答结构","讲故事"]'),

(2, NULL, 'ETIQUETTE', '面试礼仪指南：从预约到跟进',
 '着装、守时、沟通姿态、结束与感谢信，覆盖现场与线上面试的完整礼仪。',
 '# 面试礼仪指南\n\n礼仪不是形式，它传递的是"你是否把这件事当回事"。\n\n## 面试前\n- 提前 10–15 分钟到达（线上则提前调试设备与网络）\n- 研究公司与岗位，准备 2–3 个有质量的问题\n- 着装匹配公司文化，宁可略正式也不要随意\n\n## 面试中\n- 进门问候、握手（如适用）坚定自然\n- 目光接触、身体前倾，展现专注\n- 手机静音，不频繁看表\n- 回答前可短暂停顿思考，比抢答更显沉稳\n\n## 面试后\n- 结束时感谢面试官的时间\n- 24 小时内发送简短感谢邮件，重申兴趣与匹配点\n- 若约定了反馈时间未收到，可礼貌跟进一次\n\n## 线上面试额外注意\n- 背景整洁、光线充足、镜头与眼睛平齐\n- 关闭无关通知，准备纸笔记录要点',
 '["礼仪","沟通","职场"]'),

(3, NULL, 'GENERAL', '常见面试陷阱与应对',
 '被问缺点、空窗期、离职原因时，如何诚实又不自损。',
 '# 常见面试陷阱与应对\n\n## "你的缺点是什么？"\n不要说"我太追求完美"。选一个真实、已意识到且在改进的能力型短板，并说明你正在怎么做。\n\n## "为什么离开上一份工作？"\n聚焦"向往什么"而非"讨厌什么"。避免吐槽前公司或上级。\n\n## "你最大的失败？"\n用 STAR 讲一个你负主要责任、但已复盘并成长的事例，强调学到的具体东西。\n\n## 薪资期望\n可先反问岗位预算区间，给出基于市场与能力的弹性范围，避免率先报死数。\n\n## 空窗期\n如实说明期间的学习、休息或项目，把空白转化为有准备的状态。',
 '["陷阱题","话术","应变"]'),

(4, NULL, 'BEHAVIOR', '技术面试的表达与白板技巧',
 '把解题思路讲给面试官听，比闷头写代码更得分。',
 '# 技术面试表达技巧\n\n## 先澄清再动手\n拿到题目先确认输入输出、边界与规模，避免方向跑偏。\n\n## 说出思路\n边写边讲：你选了什么数据结构、复杂度如何、有没有更优解。面试官买的是思考过程。\n\n## 控制复杂度\n先给出能跑的正确解，再优化。主动说明时间/空间复杂度。\n\n## 遇到卡壳\n把卡点说出来，给出次优方案或需要的提示，比沉默更有信息量。\n\n## 结束前自查\n跑一个例子、检查边界与空输入，主动指出可能的 bug。',
 '["技术面试","白板","沟通"]');
