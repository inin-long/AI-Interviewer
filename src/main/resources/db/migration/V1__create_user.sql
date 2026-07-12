CREATE TABLE user (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      VARCHAR(64)  NOT NULL COLLATE NOCASE,
    password_hash VARCHAR(255) NOT NULL,
    nickname      VARCHAR(64)  NOT NULL,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       INTEGER      NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    CONSTRAINT uk_user_username UNIQUE (username)
);

CREATE INDEX idx_user_deleted ON user(deleted);

