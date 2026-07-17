CREATE TABLE scenario_session (
    id                   TEXT        PRIMARY KEY,
    user_id              INTEGER     NOT NULL,
    interview_session_id INTEGER     NOT NULL,
    scenario_type        VARCHAR(64) NOT NULL,
    status               VARCHAR(32) NOT NULL
        CHECK (status IN ('ACTIVE', 'COMPLETED', 'FAILED', 'ABORTED')),
    state_json           TEXT        NOT NULL,
    current_round        INTEGER     NOT NULL DEFAULT 0 CHECK (current_round >= 0),
    create_time          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (interview_session_id) REFERENCES interview_session(id)
);

CREATE UNIQUE INDEX uk_scenario_session_active
    ON scenario_session(user_id, interview_session_id)
    WHERE status = 'ACTIVE';
CREATE INDEX idx_scenario_session_user_interview
    ON scenario_session(user_id, interview_session_id, create_time);
