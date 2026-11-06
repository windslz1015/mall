-- 支付单的一次渠道发起行为；失败后可新建尝试，不重复创建支付单。
-- 一次尝试唯一的 request_no 用于渠道请求幂等。
CREATE TABLE payment_attempt (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付尝试ID',
    payment_order_id BIGINT UNSIGNED NOT NULL COMMENT '所属支付单ID',
    attempt_no INT UNSIGNED NOT NULL COMMENT '该支付单的第几次尝试，从1开始',
    request_no VARCHAR(64) NOT NULL COMMENT '平台本次支付尝试请求号（全局唯一）',
    channel_code VARCHAR(32) NOT NULL COMMENT '支付渠道代码，不能当成SDK密钥',
    channel_request_no VARCHAR(128) DEFAULT NULL COMMENT '渠道侧发起请求号（可为空）',
    channel_trade_no VARCHAR(128) DEFAULT NULL COMMENT '渠道侧交易号（可能到通知阶段才获得）',
    request_amount DECIMAL(18,2) NOT NULL COMMENT '本次发起金额（CNY元）',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0创建 10处理中 20成功 30失败 40关闭',
    failure_code VARCHAR(64) DEFAULT NULL COMMENT '脱敏后的失败原因代码',
    failure_summary VARCHAR(500) DEFAULT NULL COMMENT '脱敏后的失败摘要',
    finished_at DATETIME(3) DEFAULT NULL COMMENT '渠道尝试处理结束时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_attempt_order_seq (payment_order_id, attempt_no),
    UNIQUE KEY uk_payment_attempt_request_no (request_no),
    UNIQUE KEY uk_payment_attempt_channel_request (channel_code, channel_request_no),
    UNIQUE KEY uk_payment_attempt_id_order (id, payment_order_id),
    KEY idx_payment_attempt_order_status (payment_order_id, status, created_at),
    CONSTRAINT ck_payment_attempt_seq CHECK (attempt_no > 0),
    CONSTRAINT ck_payment_attempt_status CHECK (status IN (0, 10, 20, 30, 40)),
    CONSTRAINT ck_payment_attempt_amount CHECK (request_amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='支付渠道发起尝试及处理结果';
