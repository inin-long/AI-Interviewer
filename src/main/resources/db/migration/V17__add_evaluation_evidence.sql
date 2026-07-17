CREATE TABLE evaluation_evidence (
    id                     TEXT        PRIMARY KEY,
    user_id                INTEGER     NOT NULL,
    session_id             INTEGER     NOT NULL,
    message_id             INTEGER     NOT NULL,
    competency_code        VARCHAR(64) NOT NULL,
    signal                 VARCHAR(32) NOT NULL,
    strength               REAL        NOT NULL CHECK (strength BETWEEN 0.0 AND 1.0),
    confidence             REAL        NOT NULL CHECK (confidence BETWEEN 0.0 AND 1.0),
    reason                 TEXT        NOT NULL,
    related_claim_ids_json TEXT        NOT NULL DEFAULT '[]',
    create_time            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (session_id) REFERENCES interview_session(id),
    FOREIGN KEY (message_id) REFERENCES message(id),
    CONSTRAINT uk_evidence_message_competency_reason
        UNIQUE (session_id, message_id, competency_code, reason)
);

CREATE INDEX idx_evaluation_evidence_user_session_competency
    ON evaluation_evidence(user_id, session_id, competency_code, create_time);
CREATE INDEX idx_evaluation_evidence_message
    ON evaluation_evidence(user_id, message_id);
