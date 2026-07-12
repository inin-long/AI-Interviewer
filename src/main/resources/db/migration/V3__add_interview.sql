CREATE TABLE interview_plan (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id           INTEGER      NOT NULL,
    name              VARCHAR(128) NOT NULL,
    job_title         VARCHAR(128) NOT NULL,
    job_description   TEXT,
    difficulty        VARCHAR(32)  NOT NULL DEFAULT 'MEDIUM'
                                     CHECK (difficulty IN ('JUNIOR', 'MEDIUM', 'SENIOR', 'EXPERT')),
    duration_minutes  INTEGER      NOT NULL DEFAULT 45 CHECK (duration_minutes BETWEEN 10 AND 240),
    question_count    INTEGER      NOT NULL DEFAULT 15 CHECK (question_count BETWEEN 1 AND 100),
    resume_id         INTEGER,
    rules_json        TEXT         NOT NULL DEFAULT '{}',
    stages_json       TEXT         NOT NULL DEFAULT '[]',
    is_default        INTEGER      NOT NULL DEFAULT 0 CHECK (is_default IN (0, 1)),
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER      NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (resume_id) REFERENCES resume(id)
);

CREATE INDEX idx_interview_plan_user ON interview_plan(user_id, deleted, update_time);

CREATE TABLE interview_session (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id             INTEGER      NOT NULL,
    plan_id             INTEGER,
    resume_id           INTEGER,
    title               VARCHAR(128) NOT NULL,
    job_title           VARCHAR(128),
    plan_snapshot_json  TEXT         NOT NULL DEFAULT '{}',
    stage               VARCHAR(64)  NOT NULL DEFAULT 'INTRODUCTION',
    status              VARCHAR(32)  NOT NULL DEFAULT 'CREATED'
                                      CHECK (status IN ('CREATED', 'RUNNING', 'PAUSED', 'COMPLETED', 'FAILED')),
    prompt_version      VARCHAR(32)  NOT NULL DEFAULT 'v1.0',
    started_time        DATETIME,
    completed_time      DATETIME,
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER      NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (plan_id) REFERENCES interview_plan(id),
    FOREIGN KEY (resume_id) REFERENCES resume(id)
);

CREATE INDEX idx_interview_session_user_status
    ON interview_session(user_id, status, deleted, update_time);

CREATE TABLE message (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER     NOT NULL,
    session_id    INTEGER     NOT NULL,
    sequence_no   INTEGER     NOT NULL,
    role          VARCHAR(32) NOT NULL CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM')),
    content       TEXT        NOT NULL,
    metadata_json TEXT        NOT NULL DEFAULT '{}',
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       INTEGER     NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (session_id) REFERENCES interview_session(id),
    CONSTRAINT uk_message_sequence UNIQUE (session_id, sequence_no)
);

CREATE INDEX idx_message_user_session ON message(user_id, session_id, deleted, sequence_no);

