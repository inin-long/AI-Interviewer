CREATE TABLE task (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id        INTEGER     NOT NULL,
    task_type      VARCHAR(64) NOT NULL
                                CHECK (task_type IN ('RESUME_PARSE', 'DOCUMENT_PARSE',
                                                     'EMBEDDING_GENERATE', 'VECTOR_UPDATE',
                                                     'REPORT_GENERATE')),
    status         VARCHAR(32) NOT NULL DEFAULT 'PENDING'
                                CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED')),
    progress       INTEGER     NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
    attempt_count  INTEGER     NOT NULL DEFAULT 0,
    payload_json   TEXT        NOT NULL DEFAULT '{}',
    error_message  TEXT,
    available_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_time   DATETIME,
    finished_time  DATETIME,
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted        INTEGER     NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE INDEX idx_task_claim ON task(status, deleted, available_time, create_time);
CREATE INDEX idx_task_user ON task(user_id, status, deleted);

