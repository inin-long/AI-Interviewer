UPDATE document
SET category = '技术资料'
WHERE category IS NULL OR LENGTH(TRIM(category)) = 0;

CREATE TABLE knowledge_category (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER      NOT NULL,
    name        VARCHAR(64)  NOT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, name),
    FOREIGN KEY (user_id) REFERENCES user(id)
);

INSERT OR IGNORE INTO knowledge_category(user_id, name, create_time)
SELECT DISTINCT user_id, TRIM(category), MIN(create_time)
FROM document
WHERE deleted = 0
GROUP BY user_id, TRIM(category);

CREATE INDEX idx_knowledge_category_user
    ON knowledge_category(user_id, create_time, name);

CREATE TABLE interview_plan_category (
    plan_id     INTEGER     NOT NULL,
    category    VARCHAR(64) NOT NULL,
    user_id     INTEGER     NOT NULL,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (plan_id, category),
    FOREIGN KEY (plan_id) REFERENCES interview_plan(id),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (user_id, category) REFERENCES knowledge_category(user_id, name)
);

INSERT OR IGNORE INTO interview_plan_category(plan_id, category, user_id, create_time)
SELECT DISTINCT link.plan_id, document.category, link.user_id, link.create_time
FROM interview_plan_document link
JOIN document ON document.id = link.document_id
WHERE document.deleted = 0;

CREATE INDEX idx_plan_category_user
    ON interview_plan_category(user_id, plan_id);

DROP TABLE interview_plan_document;

CREATE TRIGGER trg_document_category_required_insert
BEFORE INSERT ON document
WHEN NEW.category IS NULL OR LENGTH(TRIM(NEW.category)) = 0
BEGIN
    SELECT RAISE(ABORT, 'document category is required');
END;

CREATE TRIGGER trg_document_category_required_update
BEFORE UPDATE OF category ON document
WHEN NEW.category IS NULL OR LENGTH(TRIM(NEW.category)) = 0
BEGIN
    SELECT RAISE(ABORT, 'document category is required');
END;
