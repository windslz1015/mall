-- 商家子订单：一次主订单中每个商家最多有一个子订单。
-- merchant_id 为 merchant-service 商家主体ID，不是商家登录账号ID。
-- merchant_name_snapshot 保留创建时的经营主体名称，避免历史记录随商家改名变化。
CREATE TABLE trade_sub_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商家子订单ID',
    sub_order_no VARCHAR(64) NOT NULL COMMENT '商家子订单号（全局唯一）',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '所属交易主订单ID（同库外键）',
    merchant_id BIGINT UNSIGNED NOT NULL COMMENT 'merchant-service商家主体ID（逻辑引用）',
    merchant_no_snapshot VARCHAR(32) NOT NULL COMMENT '创建子订单时商家业务编号快照',
    merchant_name_snapshot VARCHAR(128) NOT NULL COMMENT '创建子订单时商家名称快照',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0创建中 10待支付 20已支付 30已取消 40创建失败',
    subtotal_amount DECIMAL(18,2) NOT NULL COMMENT '本商家所有订单项金额合计',
    currency CHAR(3) NOT NULL DEFAULT 'CNY',
    paid_at DATETIME(3) DEFAULT NULL COMMENT '确认商家子订单已支付时间',
    canceled_at DATETIME(3) DEFAULT NULL COMMENT '确认商家子订单已取消时间',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_trade_sub_order_no (sub_order_no),
    UNIQUE KEY uk_trade_sub_order_parent_merchant (order_id, merchant_id),
    UNIQUE KEY uk_trade_sub_order_id_parent (id, order_id),
    KEY idx_trade_sub_order_merchant_status_time (merchant_id, status, created_at, id),
    KEY idx_trade_sub_order_merchant_time (merchant_id, created_at, id),
    CONSTRAINT fk_trade_sub_order_parent FOREIGN KEY (order_id)
        REFERENCES trade_order(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_trade_sub_order_status CHECK (status IN (0, 10, 20, 30, 40)),
    CONSTRAINT ck_trade_sub_order_amount CHECK (subtotal_amount >= 0),
    CONSTRAINT ck_trade_sub_order_status_timestamp CHECK (
        (status = 20 AND paid_at IS NOT NULL AND canceled_at IS NULL)
        OR (status = 30 AND canceled_at IS NOT NULL AND paid_at IS NULL)
        OR (status IN (0, 10, 40) AND paid_at IS NULL AND canceled_at IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='按商家拆分后的子订单，每主订单每商家一条';
