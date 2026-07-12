CREATE TABLE resume (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER      NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_name  VARCHAR(255) NOT NULL,
    storage_path  VARCHAR(512) NOT NULL,
    file_type     VARCHAR(32)  NOT NULL,
    file_size     INTEGER      NOT NULL DEFAULT 0,
    status        VARCHAR(32)  NOT NULL DEFAULT 'UPLOADED'
                               CHECK (status IN ('UPLOADED', 'PARSING', 'COMPLETED', 'FAILED')),
    error_message TEXT,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       INTEGER      NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE INDEX idx_resume_user_status ON resume(user_id, status, deleted);

CREATE TABLE candidate_profile (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    resume_id     INTEGER  NOT NULL,
    user_id       INTEGER  NOT NULL,
    content_json  TEXT     NOT NULL,
    confirmed     INTEGER  NOT NULL DEFAULT 0 CHECK (confirmed IN (0, 1)),
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       INTEGER  NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    FOREIGN KEY (resume_id) REFERENCES resume(id),
    FOREIGN KEY (user_id) REFERENCES user(id),
    CONSTRAINT uk_profile_resume UNIQUE (resume_id)
);

CREATE INDEX idx_candidate_profile_user ON candidate_profile(user_id, deleted);

