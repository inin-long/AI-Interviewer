ALTER TABLE interview_session
    ADD COLUMN knowledge_snapshot_json TEXT NOT NULL DEFAULT '[]';
