-- 交易主订单：一次用户提交形成一个主订单，可以按商家拆分多个子订单。
-- buyer_account_id 对应 iam-service 的账号ID，不做跨库外键。
-- 状态 0创建中 10待支付 20已支付 30已取消 40创建失败。
-- 本期不支持优惠券/运费/折扣，实付金额等于商品金额；以后扩展必须新建迁移。
CREATE TABLE trade_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '交易主订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '面向用户的主订单号（全局唯一）',
    buyer_account_id BIGINT UNSIGNED NOT NULL COMMENT '购买人IAM账号ID（跨服务逻辑引用）',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0创建中 10待支付 20已支付 30已取消 40创建失败',
    item_total_amount DECIMAL(18,2) NOT NULL COMMENT '商品金额合计，按订单项成交快照汇总',
    payable_amount DECIMAL(18,2) NOT NULL COMMENT '订单应付金额，必须与支付金额一致',
    currency CHAR(3) NOT NULL DEFAULT 'CNY' COMMENT '币种，当前业务使用CNY',
    pay_deadline_at DATETIME(3) NOT NULL COMMENT '支付截止时间，业务设定，不由数据库自动计算',
    paid_at DATETIME(3) DEFAULT NULL COMMENT '订单确认已支付时间',
    canceled_at DATETIME(3) DEFAULT NULL COMMENT '订单确认已取消时间',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本，业务每次状态变更递增',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_trade_order_no (order_no),
    KEY idx_trade_order_buyer_time (buyer_account_id, created_at, id),
    KEY idx_trade_order_buyer_status (buyer_account_id, status, created_at, id),
    KEY idx_trade_order_timeout (status, pay_deadline_at, id),
    CONSTRAINT ck_trade_order_status CHECK (status IN (0, 10, 20, 30, 40)),
    CONSTRAINT ck_trade_order_amount CHECK (
        item_total_amount >= 0 AND payable_amount >= 0
        AND payable_amount = item_total_amount
    ),
    CONSTRAINT ck_trade_order_status_timestamp CHECK (
        (status = 20 AND paid_at IS NOT NULL AND canceled_at IS NULL)
        OR (status = 30 AND canceled_at IS NOT NULL AND paid_at IS NULL)
        OR (status IN (0, 10, 40) AND paid_at IS NULL AND canceled_at IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='交易主订单（一次提交、一笔整体待支付交易）';
