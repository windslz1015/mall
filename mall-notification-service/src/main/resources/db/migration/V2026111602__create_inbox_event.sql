-- 每个下游服务拥有自己的消息Inbox；不可复用 IAM 服务的Outbox或队列消费位置。
CREATE TABLE inbox_event (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息去重记录ID',
    consumer_name VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '消费者逻辑名',
    event_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'IAM事件ID',
    source_service VARCHAR(64) NOT NULL COMMENT '消息来源服务',
    event_type VARCHAR(64) NOT NULL COMMENT '消息类型',
    payload_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '原始JSON字节SHA256',
    status VARCHAR(20) NOT NULL COMMENT '初始化业务成功后统一提交PROCESSED',
    processed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_inbox_consumer_event (consumer_name, event_id),
    CONSTRAINT ck_inbox_status CHECK (status IN ('PROCESSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='通知服务RabbitMQ消费幂等记录';
