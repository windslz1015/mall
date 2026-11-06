-- 每个主订单只记录一次最终成功的普通取消。
-- 仅允许从待支付(10)取消；已支付订单不能在此表登记为取消。
-- 用户取消、支付超时、系统异常取消区分来源；不表示库存已释放成功。
CREATE TABLE trade_order_cancel_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单成功取消记录ID',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '被取消的主订单ID',
    operation_id BIGINT UNSIGNED NOT NULL COMMENT '触发本次取消的幂等操作ID',
    previous_status TINYINT UNSIGNED NOT NULL DEFAULT 10 COMMENT '取消前订单状态，仅允许待支付10',
    cancel_type TINYINT UNSIGNED NOT NULL COMMENT '1用户取消 2超时取消 3系统异常取消',
    actor_account_id BIGINT UNSIGNED DEFAULT NULL COMMENT '用户取消时的IAM账号ID，系统取消时为空',
    reason_code VARCHAR(64) NOT NULL COMMENT '取消原因业务编码',
    reason_detail VARCHAR(500) DEFAULT NULL COMMENT '取消原因说明（非敏感信息）',
    canceled_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_trade_order_cancel_order (order_id),
    UNIQUE KEY uk_trade_order_cancel_operation (operation_id),
    KEY idx_trade_order_cancel_type_time (cancel_type, canceled_at, id),
    CONSTRAINT ck_trade_order_cancel_previous_status CHECK (previous_status = 10),
    CONSTRAINT ck_trade_order_cancel_type CHECK (cancel_type IN (1, 2, 3)),
    CONSTRAINT ck_trade_order_cancel_actor CHECK (
        (cancel_type = 1 AND actor_account_id IS NOT NULL)
        OR (cancel_type IN (2, 3) AND actor_account_id IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='待支付主订单的最终取消结果及原因';
