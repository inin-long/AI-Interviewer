-- 岗位库与面试题库
CREATE TABLE job_position (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER      NOT NULL,
    title         VARCHAR(255) NOT NULL,
    department    VARCHAR(128),
    description   TEXT,
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       INTEGER      NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE INDEX idx_job_position_user ON job_position(user_id, deleted);

CREATE TABLE interview_question (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id          INTEGER      NOT NULL,
    job_id           INTEGER,
    category         VARCHAR(32)  NOT NULL DEFAULT 'TECHNICAL'
                                   CHECK (category IN ('TECHNICAL', 'BEHAVIORAL', 'SCENARIO')),
    title            VARCHAR(512) NOT NULL,
    content          TEXT         NOT NULL,
    reference_answer TEXT,
    difficulty       VARCHAR(32)  NOT NULL DEFAULT 'MEDIUM',
    create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER      NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (job_id) REFERENCES job_position(id)
);

CREATE INDEX idx_interview_question_user ON interview_question(user_id, deleted);
CREATE INDEX idx_interview_question_job ON interview_question(user_id, job_id, deleted);

CREATE TABLE question_tag (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER      NOT NULL,
    name          VARCHAR(128) NOT NULL,
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       INTEGER      NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    FOREIGN KEY (user_id) REFERENCES user(id),
    CONSTRAINT uk_question_tag_name UNIQUE (user_id, name)
);

CREATE INDEX idx_question_tag_user ON question_tag(user_id, deleted);

CREATE TABLE question_tag_rel (
    question_id   INTEGER      NOT NULL,
    tag_id        INTEGER      NOT NULL,
    user_id       INTEGER      NOT NULL,
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (question_id, tag_id),
    FOREIGN KEY (question_id) REFERENCES interview_question(id),
    FOREIGN KEY (tag_id) REFERENCES question_tag(id),
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE INDEX idx_question_tag_rel_user ON question_tag_rel(user_id, tag_id);
