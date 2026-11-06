-- 平台级失败消息追踪；不替代生产者 Outbox 和消费者 Inbox。
-- 重试动作应委托消息所属服务，不能随意重发支付/库存事件。
CREATE TABLE reliability_message_failure (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息失败记录ID',
    failure_no VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '失败记录唯一号',
    producer_service VARCHAR(64) NOT NULL COMMENT '事件生产服务',
    consumer_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '消费者唯一标识（服务+处理器/订阅方）',
    event_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '来源业务事件唯一ID',
    event_type VARCHAR(64) NOT NULL COMMENT '来源业务事件类型',
    queue_name VARCHAR(128) DEFAULT NULL COMMENT '失败事件所在队列或处理通道',
    recovery_task_id BIGINT UNSIGNED DEFAULT NULL COMMENT '关联的本地重试/补偿任务',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0待处理 10已安排重试 20已恢复 30忽略',
    failure_count INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '同一事件累计失败次数',
    last_error_code VARCHAR(64) DEFAULT NULL COMMENT '最近失败码',
    last_error_summary VARCHAR(500) DEFAULT NULL COMMENT '脱敏最近失败描述',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '追踪ID',
    first_failed_at DATETIME(3) NOT NULL COMMENT '首次失败时间',
    last_failed_at DATETIME(3) NOT NULL COMMENT '最近失败时间',
    resolved_at DATETIME(3) DEFAULT NULL COMMENT '成功恢复或人工忽略时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_reliability_message_failure_no (failure_no),
    UNIQUE KEY uk_reliability_message_failure_dedup (producer_service, consumer_key, event_id),
    KEY idx_reliability_message_failure_status (status, last_failed_at, id),
    KEY idx_reliability_message_failure_task (recovery_task_id),
    KEY idx_reliability_message_failure_event (event_id),

    CONSTRAINT ck_reliability_message_failure_status CHECK (status IN (0, 10, 20, 30)),
    CONSTRAINT ck_reliability_message_failure_count CHECK (failure_count > 0),
    CONSTRAINT ck_reliability_message_failure_time CHECK (last_failed_at >= first_failed_at),
    CONSTRAINT ck_reliability_message_failure_resolved CHECK
        (status NOT IN (20, 30) OR resolved_at IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='失败业务消息集中排查追踪；消息最终处理仍由原消费者负责';
