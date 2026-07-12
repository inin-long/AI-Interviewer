CREATE TABLE agent_checkpoint (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER      NOT NULL,
    session_id    INTEGER      NOT NULL,
    node_name     VARCHAR(128) NOT NULL,
    state_json    TEXT         NOT NULL,
    state_version VARCHAR(32)  NOT NULL,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       INTEGER      NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (session_id) REFERENCES interview_session(id)
);

CREATE INDEX idx_checkpoint_latest
    ON agent_checkpoint(user_id, session_id, deleted, create_time DESC);

