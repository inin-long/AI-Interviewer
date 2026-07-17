-- AI 职业规划与简历优化历史
CREATE TABLE career_plan (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id          INTEGER      NOT NULL,
    current_role     VARCHAR(255),
    target_role      VARCHAR(255),
    industry         VARCHAR(255),
    experience_years VARCHAR(32),
    plan_markdown    TEXT,
    create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER      NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE INDEX idx_career_plan_user ON career_plan(user_id, deleted);

CREATE TABLE resume_optimization (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id          INTEGER      NOT NULL,
    original_text    TEXT         NOT NULL,
    optimized_text   TEXT,
    highlights_json  TEXT         NOT NULL DEFAULT '[]',
    create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER      NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE INDEX idx_resume_optimization_user ON resume_optimization(user_id, deleted);
