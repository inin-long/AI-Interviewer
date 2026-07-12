CREATE TABLE document (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER      NOT NULL,
    name          VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_name  VARCHAR(255) NOT NULL,
    storage_path  VARCHAR(512) NOT NULL,
    file_type     VARCHAR(32)  NOT NULL,
    file_size     INTEGER      NOT NULL DEFAULT 0,
    category      VARCHAR(64),
    status        VARCHAR(32)  NOT NULL DEFAULT 'UPLOADED'
                               CHECK (status IN ('UPLOADED', 'PARSING', 'INDEXING', 'READY', 'FAILED')),
    error_message TEXT,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       INTEGER      NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE INDEX idx_document_user_status ON document(user_id, status, deleted);

CREATE TABLE document_chunk (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER      NOT NULL,
    document_id   INTEGER      NOT NULL,
    chunk_index   INTEGER      NOT NULL,
    content       TEXT         NOT NULL,
    token_count   INTEGER      NOT NULL DEFAULT 0,
    vector_id     VARCHAR(128),
    metadata_json TEXT         NOT NULL DEFAULT '{}',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       INTEGER      NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (document_id) REFERENCES document(id),
    CONSTRAINT uk_document_chunk UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_document_chunk_user ON document_chunk(user_id, document_id, deleted);

CREATE TABLE interview_plan_document (
    plan_id       INTEGER  NOT NULL,
    document_id   INTEGER  NOT NULL,
    user_id       INTEGER  NOT NULL,
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (plan_id, document_id),
    FOREIGN KEY (plan_id) REFERENCES interview_plan(id),
    FOREIGN KEY (document_id) REFERENCES document(id),
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE INDEX idx_plan_document_user ON interview_plan_document(user_id, plan_id);

