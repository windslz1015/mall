-- 预留订单侧事件消费去重（如订单取消、订单超时等）；与支付渠道 HTTP 回调不是同一概念。
-- HTTP 渠道回调由 payment_notification 去重。
CREATE TABLE inbox_event (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Inbox消息ID',
    consumer_name VARCHAR(64) NOT NULL COMMENT '当前消费者逻辑名',
    event_id VARCHAR(64) NOT NULL COMMENT '上游事件ID',
    source_service VARCHAR(64) NOT NULL COMMENT '上游服务名',
    event_type VARCHAR(128) NOT NULL COMMENT '上游业务事件类型',
    payload_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT '消息规范化载荷SHA-256指纹',
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING' COMMENT 'PROCESSING/PROCESSED/FAILED',
    retry_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '本地处理重试次数',
    last_error VARCHAR(500) DEFAULT NULL COMMENT '最近异常摘要（脱敏）',
    received_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    processed_at DATETIME(3) DEFAULT NULL COMMENT '本地事务处理完成时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_inbox_event_consumer_event (consumer_name, event_id),
    KEY idx_inbox_event_status_retry (status, updated_at, id),
    KEY idx_inbox_event_source_type (source_service, event_type, received_at, id),
    CONSTRAINT ck_inbox_event_status CHECK (status IN ('PROCESSING', 'PROCESSED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='支付服务异步事件收件去重（未实现消息消费）';
