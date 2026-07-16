CREATE TABLE interview_claim (
    id                            TEXT        PRIMARY KEY,
    user_id                       INTEGER     NOT NULL,
    session_id                    INTEGER     NOT NULL,
    source_message_id             INTEGER     NOT NULL,
    claim_type                    VARCHAR(32) NOT NULL,
    content                       TEXT        NOT NULL,
    importance                    REAL        NOT NULL CHECK (importance BETWEEN 0.0 AND 1.0),
    credibility                   REAL        NOT NULL CHECK (credibility BETWEEN 0.0 AND 1.0),
    status                        VARCHAR(32) NOT NULL,
    missing_evidence_json         TEXT        NOT NULL DEFAULT '[]',
    supporting_evidence_ids_json  TEXT        NOT NULL DEFAULT '[]',
    conflicting_evidence_ids_json TEXT        NOT NULL DEFAULT '[]',
    create_time                   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time                   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (session_id) REFERENCES interview_session(id),
    FOREIGN KEY (source_message_id) REFERENCES message(id),
    CONSTRAINT uk_claim_source_content UNIQUE (session_id, source_message_id, content)
);

CREATE INDEX idx_interview_claim_user_session_status
    ON interview_claim(user_id, session_id, status, importance DESC, create_time);
CREATE INDEX idx_interview_claim_source_message
    ON interview_claim(user_id, source_message_id);
