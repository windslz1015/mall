-- 只在主订单状态实际改变时追加一条记录；不能通过此表代替 trade_order 当前状态。
-- source_type 1用户 2系统任务/补偿 3支付结果；来源ID均为逻辑引用。
CREATE TABLE trade_order_status_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主订单状态变化流水ID',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '主订单ID',
    from_status TINYINT UNSIGNED DEFAULT NULL COMMENT '变化前状态，NULL表示创建',
    to_status TINYINT UNSIGNED NOT NULL COMMENT '变化后状态',
    source_type TINYINT UNSIGNED NOT NULL COMMENT '1用户 2系统 3支付结果',
    actor_account_id BIGINT UNSIGNED DEFAULT NULL COMMENT '用户操作账号ID，系统/支付结果时为空',
    reason_code VARCHAR(64) NOT NULL COMMENT '状态变更原因编码',
    source_event_id VARCHAR(64) DEFAULT NULL COMMENT '上游支付/消息事件ID（跨服务逻辑关联）',
    occurred_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_trade_order_status_log_order (order_id, occurred_at, id),
    KEY idx_trade_order_status_log_event (source_event_id),

    CONSTRAINT ck_trade_order_status_log_from CHECK (
        from_status IS NULL OR from_status IN (0, 10, 20, 30, 40)
    ),
    CONSTRAINT ck_trade_order_status_log_to CHECK (to_status IN (0, 10, 20, 30, 40)),
    CONSTRAINT ck_trade_order_status_log_changed CHECK (
        from_status IS NULL OR from_status <> to_status
    ),
    CONSTRAINT ck_trade_order_status_log_source CHECK (source_type IN (1, 2, 3)),
    CONSTRAINT ck_trade_order_status_log_actor CHECK (
        (source_type = 1 AND actor_account_id IS NOT NULL)
        OR (source_type IN (2, 3) AND actor_account_id IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='交易主订单状态流转历史（追加写）';
