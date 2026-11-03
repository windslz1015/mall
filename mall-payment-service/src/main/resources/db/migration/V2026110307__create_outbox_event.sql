-- 本地事务预留可靠消息：未来支付确认成功应同时保存支付状态和待投递事件。
-- 当前仅建表，不包含 RabbitMQ 生产者、确认回调和重试任务实现。
CREATE TABLE outbox_event (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Outbox事件ID',
    event_id VARCHAR(64) NOT NULL COMMENT '全局唯一事件ID',
    aggregate_type VARCHAR(64) NOT NULL COMMENT '聚合类型，如PaymentOrder',
    aggregate_id VARCHAR(64) NOT NULL COMMENT '聚合根业务编号',
    event_type VARCHAR(128) NOT NULL COMMENT '业务事件类型',
    payload JSON NOT NULL COMMENT '脱敏业务事件JSON，不包含支付渠道密钥',
    biz_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '同一业务事件去重键',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENDING/SENT/FAILED',
    claim_token VARCHAR(36) DEFAULT NULL COMMENT '本次投递租约标识',
    retry_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计重试次数',
    next_retry_at DATETIME(3) DEFAULT NULL COMMENT '下次发送时间',
    last_error VARCHAR(500) DEFAULT NULL COMMENT '最近一次失败摘要（脱敏）',
    sent_at DATETIME(3) DEFAULT NULL COMMENT '确认成功投递时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_event_id (event_id),
    UNIQUE KEY uk_outbox_event_biz_key (biz_key),
    KEY idx_outbox_event_status_retry (status, next_retry_at, created_at, id),
    KEY idx_outbox_event_aggregate (aggregate_type, aggregate_id),
    CONSTRAINT ck_outbox_event_status CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='支付服务Outbox预留（尚未实现RabbitMQ投递）';
