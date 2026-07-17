CREATE TABLE domain_pack (
    id            TEXT         PRIMARY KEY,
    role_code     VARCHAR(64)  NOT NULL,
    industry_code VARCHAR(64),
    display_name  VARCHAR(128) NOT NULL,
    version       VARCHAR(32)  NOT NULL,
    content_json  TEXT         NOT NULL,
    enabled       INTEGER      NOT NULL DEFAULT 1 CHECK (enabled IN (0, 1)),
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_domain_pack_role_industry_version
    ON domain_pack(role_code, COALESCE(industry_code, ''), version);
CREATE INDEX idx_domain_pack_enabled_role
    ON domain_pack(enabled, role_code, industry_code);

ALTER TABLE interview_plan
    ADD COLUMN domain_pack_id TEXT NOT NULL DEFAULT 'java-backend-1.0.0';

ALTER TABLE interview_session
    ADD COLUMN domain_pack_id TEXT NOT NULL DEFAULT 'java-backend-1.0.0';
ALTER TABLE interview_session
    ADD COLUMN domain_pack_version VARCHAR(32) NOT NULL DEFAULT '1.0.0';
ALTER TABLE interview_session
    ADD COLUMN domain_pack_snapshot_json TEXT NOT NULL DEFAULT '{}';

CREATE INDEX idx_interview_plan_domain_pack
    ON interview_plan(domain_pack_id, deleted);
CREATE INDEX idx_interview_session_domain_pack
    ON interview_session(domain_pack_id, domain_pack_version, deleted);
