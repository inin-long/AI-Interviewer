CREATE TABLE consistency_issue (
    id                       TEXT        PRIMARY KEY,
    user_id                  INTEGER     NOT NULL,
    session_id               INTEGER     NOT NULL,
    issue_type               VARCHAR(32) NOT NULL,
    status                   VARCHAR(32) NOT NULL
                                         CHECK (status IN ('POTENTIAL', 'CLARIFIED', 'RESOLVED', 'CONFIRMED_CONFLICT')),
    description              TEXT        NOT NULL,
    related_claim_ids_json   TEXT        NOT NULL DEFAULT '[]',
    clarification_message_id INTEGER,
    clarification_question   TEXT        NOT NULL DEFAULT '',
    resolution               TEXT        NOT NULL DEFAULT '',
    create_time              DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time              DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (session_id) REFERENCES interview_session(id),
    FOREIGN KEY (clarification_message_id) REFERENCES message(id),
    CONSTRAINT uk_consistency_issue_claims
        UNIQUE (session_id, issue_type, related_claim_ids_json)
);

CREATE INDEX idx_consistency_issue_user_session_status
    ON consistency_issue(user_id, session_id, status, update_time);
