-- 区分「内置知识包」与「用户自建知识包」，避免每次启动禁用用户包
ALTER TABLE domain_pack
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'BUILTIN';
CREATE INDEX idx_domain_pack_source ON domain_pack(source);
