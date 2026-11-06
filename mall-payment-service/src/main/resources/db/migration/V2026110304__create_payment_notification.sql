-- 第三方渠道回调收件箱：notification_key 应由渠道事件ID或稳定业务指纹生成。
-- 同一通知重试只保留一行，received_count 递增，不存渠道原始敏感报文/签名秘钥。
-- 安全原则：签名/渠道查单验证成功之前，不能将通知当成真实支付成功。
CREATE TABLE payment_notification (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '渠道通知主键',
    channel_code VARCHAR(32) NOT NULL COMMENT '支付渠道',
    notification_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT '同一渠道事件的稳定去重键',
    event_type VARCHAR(64) NOT NULL COMMENT '渠道回调事件类型',
    channel_trade_no VARCHAR(128) DEFAULT NULL COMMENT '渠道交易号（可为空）',
    payload_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT '规范化渠道通知内容的SHA-256指纹',
    signature_valid TINYINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '是否已通过渠道签名校验：0否1是',
    process_status TINYINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '0收到 10已校验 20已处理 30已拒绝 40处理失败',
    payment_order_id BIGINT UNSIGNED DEFAULT NULL COMMENT '已解析的支付单；无匹配时可为空',
    receive_count INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '同一通知累计接收次数',
    received_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        COMMENT '首次收到时间',
    last_received_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        COMMENT '最近收到时间',
    processed_at DATETIME(3) DEFAULT NULL COMMENT '确认处理成功时间',
    last_error VARCHAR(500) DEFAULT NULL COMMENT '脱敏的异常摘要',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_notification_channel_key (channel_code, notification_key),
    KEY idx_payment_notification_status_time (process_status, updated_at, id),
    KEY idx_payment_notification_order (payment_order_id, received_at, id),
    KEY idx_payment_notification_trade (channel_code, channel_trade_no),

    CONSTRAINT ck_payment_notification_signature CHECK (signature_valid IN (0, 1)),
    CONSTRAINT ck_payment_notification_status CHECK (process_status IN (0, 10, 20, 30, 40)),
    CONSTRAINT ck_payment_notification_receive CHECK (receive_count > 0),
    CONSTRAINT ck_payment_notification_verified CHECK
        (process_status <> 20 OR (signature_valid = 1 AND processed_at IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='支付渠道回调接收、验签结果与处理状态';
