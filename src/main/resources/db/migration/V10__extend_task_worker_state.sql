ALTER TABLE task ADD COLUMN worker_id VARCHAR(128);

CREATE INDEX idx_task_worker ON task(worker_id, status, deleted);
