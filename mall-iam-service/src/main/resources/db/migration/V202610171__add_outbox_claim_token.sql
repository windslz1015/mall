-- 以此令牌作为 Outbox 投递 ownership fencing，防止过期持有者覆盖新投递者状态。
-- 已执行的 Flyway 版本不得原地修改。
ALTER TABLE outbox_event
    ADD COLUMN claim_token VARCHAR(36) DEFAULT NULL COMMENT '当前投递租约令牌';
