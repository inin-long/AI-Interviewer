CREATE TABLE deferred_probe (
    id              TEXT        PRIMARY KEY,
    user_id         INTEGER     NOT NULL,
    session_id      INTEGER     NOT NULL,
    target_claim_id TEXT        NOT NULL,
    preferred_stage VARCHAR(64),
    strategy        VARCHAR(64) NOT NULL,
    reason          TEXT        NOT NULL,
    completed       INTEGER     NOT NULL DEFAULT 0 CHECK (completed IN (0, 1)),
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (session_id) REFERENCES interview_session(id)
);

CREATE UNIQUE INDEX uk_deferred_probe_schedule
    ON deferred_probe(session_id, target_claim_id, IFNULL(preferred_stage, ''), strategy);
CREATE INDEX idx_deferred_probe_user_session_pending
    ON deferred_probe(user_id, session_id, completed, preferred_stage, create_time);
