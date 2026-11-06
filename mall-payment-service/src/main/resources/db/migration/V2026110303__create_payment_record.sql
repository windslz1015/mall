-- 渠道已经确认成功入账的客观支付事实。一个支付单出现两笔真实成功扣款时，
-- 两笔事实都必须留存并进入异常复核，不能因 payment_order_id 唯一而丢失第二笔。
-- 不记录退款；未来退款应单独建聚合与 Flyway 迁移。
CREATE TABLE payment_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付入账事实ID',
    record_no VARCHAR(64) NOT NULL COMMENT '平台支付入账记录号',
    payment_order_id BIGINT UNSIGNED NOT NULL COMMENT '所属支付单',
    payment_attempt_id BIGINT UNSIGNED DEFAULT NULL COMMENT '可能关联的渠道支付尝试',
    channel_code VARCHAR(32) NOT NULL COMMENT '支付渠道代码',
    channel_trade_no VARCHAR(128) NOT NULL COMMENT '渠道确认成功的唯一交易号',
    paid_amount DECIMAL(18,2) NOT NULL COMMENT '渠道确认实付金额（CNY元）',
    currency CHAR(3) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    channel_paid_at DATETIME(3) DEFAULT NULL COMMENT '渠道报告的实际支付时间',
    confirmed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        COMMENT '平台确认渠道事实的时间',
    review_status TINYINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '0正常 10关闭后成功 20重复成功扣款 30金额不一致 40待人工复核',
    review_note VARCHAR(500) DEFAULT NULL COMMENT '脱敏的复核备注',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_record_no (record_no),
    UNIQUE KEY uk_payment_record_channel_trade (channel_code, channel_trade_no),
    KEY idx_payment_record_order_confirmed (payment_order_id, confirmed_at, id),
    KEY idx_payment_record_review (review_status, confirmed_at, id),

    CONSTRAINT ck_payment_record_amount CHECK (paid_amount > 0),
    CONSTRAINT ck_payment_record_currency CHECK (currency = 'CNY'),
    CONSTRAINT ck_payment_record_review CHECK (review_status IN (0, 10, 20, 30, 40))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='第三方渠道已成功扣款的事实记录；允许异常重复扣款留痕';
