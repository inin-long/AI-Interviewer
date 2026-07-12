CREATE TABLE evaluation (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id               INTEGER NOT NULL,
    interview_id          INTEGER NOT NULL,
    overall_score         INTEGER NOT NULL CHECK (overall_score BETWEEN 0 AND 100),
    technical_score       INTEGER NOT NULL CHECK (technical_score BETWEEN 0 AND 100),
    problem_solving_score INTEGER NOT NULL CHECK (problem_solving_score BETWEEN 0 AND 100),
    project_score         INTEGER NOT NULL CHECK (project_score BETWEEN 0 AND 100),
    system_design_score   INTEGER NOT NULL CHECK (system_design_score BETWEEN 0 AND 100),
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

CREATE INDEX idx_evaluation_user ON evaluation(user_id, deleted, create_time);

CREATE TABLE report (
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
    FOREIGN KEY (evaluation_id) REFERENCES evaluation(id),
    CONSTRAINT uk_report_interview UNIQUE (interview_id)
);

CREATE INDEX idx_report_user_status ON report(user_id, status, deleted, create_time);

