ALTER TABLE interview_plan ADD COLUMN profile_id INTEGER REFERENCES candidate_profile(id);

ALTER TABLE interview_session ADD COLUMN profile_id INTEGER REFERENCES candidate_profile(id);
ALTER TABLE interview_session ADD COLUMN profile_snapshot_json TEXT NOT NULL DEFAULT '{}';

CREATE INDEX idx_interview_plan_profile
    ON interview_plan(user_id, profile_id, deleted);

CREATE INDEX idx_interview_session_profile
    ON interview_session(user_id, profile_id, deleted);
