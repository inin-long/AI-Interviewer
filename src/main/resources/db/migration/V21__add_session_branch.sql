CREATE TABLE session_branch (
    id                     TEXT         PRIMARY KEY,
    user_id                INTEGER      NOT NULL,
    source_session_id      INTEGER      NOT NULL,
    source_checkpoint_id   INTEGER      NOT NULL,
    parent_branch_id       TEXT,
    source_question_number INTEGER      NOT NULL CHECK (source_question_number > 0),
    title                  VARCHAR(128) NOT NULL,
    status                 VARCHAR(32)  NOT NULL
                                      CHECK (status IN ('DRAFT', 'PROCESSING', 'COMPLETED', 'FAILED')),
    source_state_json      TEXT         NOT NULL,
    original_question      TEXT         NOT NULL,
    original_answer        TEXT         NOT NULL,
    new_answer             TEXT         NOT NULL DEFAULT '',
    comparison_json        TEXT         NOT NULL DEFAULT '{}',
    comparison_markdown    TEXT         NOT NULL DEFAULT '',
    error_message          VARCHAR(500) NOT NULL DEFAULT '',
    create_time            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                INTEGER      NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (source_session_id) REFERENCES interview_session(id),
    FOREIGN KEY (source_checkpoint_id) REFERENCES agent_checkpoint(id),
    FOREIGN KEY (parent_branch_id) REFERENCES session_branch(id)
);

CREATE INDEX idx_session_branch_user_source
    ON session_branch(user_id, source_session_id, deleted, create_time DESC);
CREATE INDEX idx_session_branch_checkpoint
    ON session_branch(user_id, source_checkpoint_id, deleted);
