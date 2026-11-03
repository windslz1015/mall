-- 与 IAM 服务的事务性 Outbox 思路保持一致：订单本地事务写业务记录+待投递事件。
-- 本文件只建表，不实现 RabbitMQ 生产者、调度、投递重试和确认回调。
CREATE TABLE outbox_event (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Outbox事件主键',
    event_id VARCHAR(64) NOT NULL COMMENT '全局唯一业务事件ID',
    aggregate_type VARCHAR(64) NOT NULL COMMENT '聚合类型，如TradeOrder',
    aggregate_id VARCHAR(64) NOT NULL COMMENT '聚合根业务标识',
    event_type VARCHAR(128) NOT NULL COMMENT '事件类型',
    payload JSON NOT NULL COMMENT '业务事件JSON内容',
    biz_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT '同一业务事件幂等键',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENDING/SENT/FAILED',
    claim_token VARCHAR(36) DEFAULT NULL COMMENT '本次消息投递租约令牌',
    retry_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_retry_at DATETIME(3) DEFAULT NULL COMMENT '下次重试时间',
    last_error VARCHAR(500) DEFAULT NULL COMMENT '最近一次发送失败摘要',
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
  COMMENT='订单事务性Outbox事件表（仅表结构，未实现投递）';
