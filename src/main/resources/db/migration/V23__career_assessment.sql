-- 职业测评：霍兰德 / MBTI 问卷与结果
CREATE TABLE assessment_template (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    code        VARCHAR(32) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_assessment_template_code UNIQUE (code)
);

CREATE TABLE assessment_question (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    template_id   INTEGER      NOT NULL,
    dimension     VARCHAR(32)  NOT NULL,
    content       TEXT         NOT NULL,
    options_json  TEXT         NOT NULL,
    sort_order    INTEGER      NOT NULL DEFAULT 0,
    FOREIGN KEY (template_id) REFERENCES assessment_template(id)
);

CREATE INDEX idx_assessment_question_template ON assessment_question(template_id, sort_order);

CREATE TABLE assessment_result (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER      NOT NULL,
    template_code   VARCHAR(32)  NOT NULL,
    result_code     VARCHAR(32),
    scores_json     TEXT         NOT NULL,
    report_markdown TEXT,
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER      NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE INDEX idx_assessment_result_user ON assessment_result(user_id, deleted);

CREATE TABLE assessment_answer (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    result_id     INTEGER      NOT NULL,
    question_id   INTEGER      NOT NULL,
    option_index  INTEGER      NOT NULL,
    FOREIGN KEY (result_id) REFERENCES assessment_result(id),
    FOREIGN KEY (question_id) REFERENCES assessment_question(id)
);

CREATE INDEX idx_assessment_answer_result ON assessment_answer(result_id);

-- 种子数据：霍兰德 RIASEC（12 题，每题二选一，落到不同兴趣类型）
INSERT INTO assessment_template(id, code, title, description) VALUES
(1, 'HOLLAND', '霍兰德职业兴趣测验', '通过活动偏好判断你的六种职业兴趣类型（现实/研究/艺术/社会/企业/常规），取得分最高的三项组成兴趣代码。');

INSERT INTO assessment_question(template_id, dimension, sort_order, content, options_json) VALUES
(1, 'HOLLAND', 1, '你更愿意：', '[{"label":"维修或操作机械设备","score":"R"},{"label":"研究抽象问题或做实验","score":"I"}]'),
(1, 'HOLLAND', 2, '你更愿意：', '[{"label":"创作绘画、音乐或文字作品","score":"A"},{"label":"说服他人接受你的方案","score":"E"}]'),
(1, 'HOLLAND', 3, '你更愿意：', '[{"label":"帮助他人解决困难","score":"S"},{"label":"整理、归档与核对数据","score":"C"}]'),
(1, 'HOLLAND', 4, '你更愿意：', '[{"label":"分析实验与统计数据","score":"I"},{"label":"设计有创意的作品","score":"A"}]'),
(1, 'HOLLAND', 5, '你更愿意：', '[{"label":"带领团队达成目标","score":"E"},{"label":"动手组装或加工设备","score":"R"}]'),
(1, 'HOLLAND', 6, '你更愿意：', '[{"label":"按流程录入与维护信息","score":"C"},{"label":"培训并指导新人","score":"S"}]'),
(1, 'HOLLAND', 7, '你更愿意：', '[{"label":"使用工具加工零件","score":"R"},{"label":"按制度录入与核对信息","score":"C"}]'),
(1, 'HOLLAND', 8, '你更愿意：', '[{"label":"探索科学原理","score":"I"},{"label":"倾听并支持他人","score":"S"}]'),
(1, 'HOLLAND', 9, '你更愿意：', '[{"label":"编写故事或剧本","score":"A"},{"label":"组织活动并拉来赞助","score":"E"}]'),
(1, 'HOLLAND', 10, '你更愿意：', '[{"label":"调解人际冲突","score":"S"},{"label":"制定销售策略","score":"E"}]'),
(1, 'HOLLAND', 11, '你更愿意：', '[{"label":"维护数据库的准确性","score":"C"},{"label":"推导数学或自然规律","score":"I"}]'),
(1, 'HOLLAND', 12, '你更愿意：', '[{"label":"表演或展示才艺","score":"A"},{"label":"修理家用器具","score":"R"}]');

-- 种子数据：MBTI（12 题，每题二选一，落到四种二元维度）
INSERT INTO assessment_template(id, code, title, description) VALUES
(2, 'MBTI', 'MBTI 性格类型测验', '从精力导向、信息获取、决策方式、生活态度四个维度判断你的性格类型（如 INTJ、ESFP）。');

INSERT INTO assessment_question(template_id, dimension, sort_order, content, options_json) VALUES
(2, 'MBTI', 1, '聚会中你通常：', '[{"label":"结识新朋友让你更有活力","score":"E"},{"label":"独处充电让你更舒服","score":"I"}]'),
(2, 'MBTI', 2, '表达时你更习惯：', '[{"label":"先说出口再整理思路","score":"E"},{"label":"先在脑中想清楚再说","score":"I"}]'),
(2, 'MBTI', 3, '你更关注：', '[{"label":"具体的事实与细节","score":"S"},{"label":"未来的可能与含义","score":"N"}]'),
(2, 'MBTI', 4, '你更喜欢：', '[{"label":"按部就班、有条不紊","score":"S"},{"label":"灵活变化、随机应变","score":"N"}]'),
(2, 'MBTI', 5, '做决定时你更看重：', '[{"label":"逻辑与客观因果","score":"T"},{"label":"感受与他人处境","score":"F"}]'),
(2, 'MBTI', 6, '你更倾向：', '[{"label":"对事不对人","score":"T"},{"label":"在意对方感受","score":"F"}]'),
(2, 'MBTI', 7, '面对任务你更：', '[{"label":"先列计划再执行","score":"J"},{"label":"随性而动、留有余地","score":"P"}]'),
(2, 'MBTI', 8, '你更希望：', '[{"label":"尽早得到确定结论","score":"J"},{"label":"保持开放、再看情况","score":"P"}]'),
(2, 'MBTI', 9, '结识新环境让你：', '[{"label":"兴奋且主动","score":"E"},{"label":"消耗精力、想回避","score":"I"}]'),
(2, 'MBTI', 10, '你更相信：', '[{"label":"已验证的经验","score":"S"},{"label":"直觉与灵感","score":"N"}]'),
(2, 'MBTI', 11, '冲突中你更：', '[{"label":"追求客观公正","score":"T"},{"label":"追求关系和谐","score":"F"}]'),
(2, 'MBTI', 12, 'deadline 前你通常：', '[{"label":"提前完成","score":"J"},{"label":"拖到最后时刻","score":"P"}]');
