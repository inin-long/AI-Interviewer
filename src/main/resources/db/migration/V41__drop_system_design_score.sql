-- 删除“系统设计”评分维度：将能力维度从六维精简为五维。
-- evaluation.system_design_score 带有列级 CHECK 约束，SQLite 的 DROP COLUMN
-- 无法直接删除，因此采用“建新表 -> 拷数据 -> 删旧表 -> 改名”的标准重建方式。
-- 由于 report.evaluation_id 外键引用 evaluation(id)，单独重建 evaluation 会在 DROP 时触发外键校验失败；
-- 因此本迁移同时重建 report 表，report_new 先引用 evaluation_new，待两张表都重命名后，
-- SQLite 会自动把外键引用修正到重命名后的 evaluation。

DROP TABLE IF EXISTS evaluation_new;
DROP TABLE IF EXISTS report_new;

CREATE TABLE evaluation_new (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id               INTEGER NOT NULL,
    interview_id          INTEGER NOT NULL,
    overall_score         INTEGER NOT NULL CHECK (overall_score BETWEEN 0 AND 100),
    technical_score       INTEGER NOT NULL CHECK (technical_score BETWEEN 0 AND 100),
    problem_solving_score INTEGER NOT NULL CHECK (problem_solving_score BETWEEN 0 AND 100),
    project_score         INTEGER NOT NULL CHECK (project_score BETWEEN 0 AND 100),
    communication_score   INTEGER NOT NULL CHECK (communication_score BETWEEN 0 AND 100),
    comprehensive_score   INTEGER NOT NULL CHECK (comprehensive_score BETWEEN 0 AND 100),
    content_json          TEXT    NOT NULL DEFAULT '{}',
    create_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               INTEGER NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (interview_id) REFERENCES interview_session(id),
    CONSTRAINT uk_evaluation_interview UNIQUE (interview_id)
);

INSERT INTO evaluation_new (id, user_id, interview_id, overall_score, technical_score,
                            problem_solving_score, project_score, communication_score,
                            comprehensive_score, content_json, create_time, update_time, deleted)
SELECT id, user_id, interview_id, overall_score, technical_score,
       problem_solving_score, project_score, communication_score,
       comprehensive_score, content_json, create_time, update_time, deleted
FROM evaluation;

CREATE TABLE report_new (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id          INTEGER      NOT NULL,
    interview_id     INTEGER      NOT NULL,
    evaluation_id    INTEGER,
    title            VARCHAR(255) NOT NULL,
    content_markdown TEXT,
    score            INTEGER CHECK (score BETWEEN 0 AND 100),
    status           VARCHAR(32)  NOT NULL DEFAULT 'GENERATING'
                                  CHECK (status IN ('GENERATING', 'COMPLETED', 'FAILED')),
    storage_path     VARCHAR(512),
    error_message    TEXT,
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER      NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (interview_id) REFERENCES interview_session(id),
    FOREIGN KEY (evaluation_id) REFERENCES evaluation_new(id),
    CONSTRAINT uk_report_interview UNIQUE (interview_id)
);

INSERT INTO report_new (id, user_id, interview_id, evaluation_id, title, content_markdown,
                        score, status, storage_path, error_message, create_time, update_time, deleted)
SELECT id, user_id, interview_id, evaluation_id, title, content_markdown,
       score, status, storage_path, error_message, create_time, update_time, deleted
FROM report;

DROP TABLE report;
DROP TABLE evaluation;

ALTER TABLE evaluation_new RENAME TO evaluation;
ALTER TABLE report_new RENAME TO report;

CREATE INDEX idx_evaluation_user ON evaluation(user_id, deleted, create_time);
CREATE INDEX idx_report_user_status ON report(user_id, status, deleted, create_time);
