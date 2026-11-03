-- 一笔交易主订单对应一笔支付单；多次发起/重试通过 payment_attempt 记录。
-- trade_order_id、buyer_account_id 分别引用订单/IAM 服务业务 ID，不建立跨库外键。
-- 0 CREATED 10 PENDING 20 SUCCEEDED 30 FAILED 40 CLOSED 50 EXCEPTION。
CREATE TABLE payment_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付单主键ID',
    payment_no VARCHAR(64) NOT NULL COMMENT '平台侧唯一支付单号',
    trade_order_id BIGINT UNSIGNED NOT NULL COMMENT 'order-service 交易主订单ID（逻辑引用）',
    trade_order_no VARCHAR(64) NOT NULL COMMENT '交易主订单号快照（便于查询和对账）',
    buyer_account_id BIGINT UNSIGNED NOT NULL COMMENT 'IAM普通用户账号ID（逻辑引用）',
    payable_amount DECIMAL(18,2) NOT NULL COMMENT '支付单应付金额（CNY元）',
    currency CHAR(3) NOT NULL DEFAULT 'CNY' COMMENT '当前仅支持人民币',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0创建 10待支付 20成功 30失败 40关闭 50异常',
    pay_deadline_at DATETIME(3) NOT NULL COMMENT '订单支付截止时间快照',
    paid_at DATETIME(3) DEFAULT NULL COMMENT '确认渠道已支付的时间',
    closed_at DATETIME(3) DEFAULT NULL COMMENT '正常关闭时间',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号；业务状态变更需递增',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_order_no (payment_no),
    UNIQUE KEY uk_payment_order_trade_order (trade_order_id),
    KEY idx_payment_order_buyer_created (buyer_account_id, created_at, id),
    KEY idx_payment_order_status_deadline (status, pay_deadline_at, id),
    KEY idx_payment_order_trade_no (trade_order_no),
    CONSTRAINT ck_payment_order_status CHECK (status IN (0, 10, 20, 30, 40, 50)),
    CONSTRAINT ck_payment_order_amount CHECK (payable_amount > 0),
    CONSTRAINT ck_payment_order_currency CHECK (currency = 'CNY'),
    CONSTRAINT ck_payment_order_paid_at CHECK (status <> 20 OR paid_at IS NOT NULL),
    CONSTRAINT ck_payment_order_closed_at CHECK (status <> 40 OR closed_at IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='支付单：一笔主订单对应一笔支付单，允许多次渠道支付尝试';
