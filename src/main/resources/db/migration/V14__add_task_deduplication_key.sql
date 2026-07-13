ALTER TABLE task ADD COLUMN deduplication_key VARCHAR(255);

CREATE UNIQUE INDEX uk_task_active_deduplication
    ON task(user_id, task_type, deduplication_key)
    WHERE deleted = 0
      AND deduplication_key IS NOT NULL
      AND status IN ('PENDING', 'RUNNING');
