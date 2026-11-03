CREATE TABLE outbox_event (

    id BIGINT UNSIGNED NOT NULL
        COMMENT '事件主键ID',

    event_id VARCHAR(64) NOT NULL
        COMMENT '全局唯一事件标识',

    aggregate_type VARCHAR(64) NOT NULL
        COMMENT '聚合类型',

    aggregate_id VARCHAR(64) NOT NULL
        COMMENT '聚合标识',

    event_type VARCHAR(128) NOT NULL
        COMMENT '事件类型',

    payload JSON NOT NULL
        COMMENT '事件JSON数据',

    status VARCHAR(20) NOT NULL
        DEFAULT 'PENDING'
        COMMENT 'PENDING/SENDING/SENT/FAILED',

    retry_count INT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '已重试次数',

    next_retry_at DATETIME(3) DEFAULT NULL
        COMMENT '下次重试时间',

    created_at DATETIME(3) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(3),

    updated_at DATETIME(3) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),

    sent_at DATETIME(3) DEFAULT NULL
        COMMENT '成功发送时间',

    biz_key VARCHAR(128) NOT NULL
    COMMENT '业务幂等键',

    PRIMARY KEY (id),

    UNIQUE KEY uk_event_id (event_id),

    UNIQUE KEY uk_biz_key (biz_key),

    KEY idx_status_retry (
        status,
        next_retry_at,
        created_at
    ),

    KEY idx_aggregate (
        aggregate_type,
        aggregate_id
    )

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COMMENT='Outbox本地消息事件表';