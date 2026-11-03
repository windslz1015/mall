-- 未来消费者去重：同一 consumer_name + event_id 只能占用一条收件记录。
-- 消费者须校验重复消息的 payload_sha256 并以本地事务一起提交订单状态变更。
-- 本文件仅建表；未实现 RabbitMQ 消费、状态机和消息重试。
CREATE TABLE inbox_event (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Inbox收件记录主键',
    consumer_name VARCHAR(64) NOT NULL COMMENT '订单服务消费者逻辑名称',
    event_id VARCHAR(64) NOT NULL COMMENT '上游事件全局唯一ID',
    source_service VARCHAR(64) NOT NULL COMMENT '上游服务名',
    event_type VARCHAR(128) NOT NULL COMMENT '上游事件类型',
    payload_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT '收到事件负载规范化SHA-256指纹',
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING' COMMENT 'PROCESSING/PROCESSED/FAILED',
    retry_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '本地消费重试次数',
    last_error VARCHAR(500) DEFAULT NULL COMMENT '最近消费错误摘要',
    received_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    processed_at DATETIME(3) DEFAULT NULL COMMENT '确认本地消费成功时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_inbox_event_consumer_event (consumer_name, event_id),
    KEY idx_inbox_event_status_retry (status, updated_at, id),
    KEY idx_inbox_event_source_type (source_service, event_type, received_at, id),
    CONSTRAINT ck_inbox_event_status CHECK (status IN ('PROCESSING', 'PROCESSED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='订单消费方Inbox去重记录（仅表结构，未实现消费）';
