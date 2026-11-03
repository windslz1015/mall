-- 商家子订单单独记录状态变化历史；支付/取消阶段应与交易主订单一致。
-- 仅依赖本服务子订单表，不跨库关联商家或支付表。
CREATE TABLE trade_sub_order_status_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商家子订单状态变化流水ID',
    sub_order_id BIGINT UNSIGNED NOT NULL COMMENT '商家子订单ID',
    from_status TINYINT UNSIGNED DEFAULT NULL COMMENT '变化前状态，NULL表示创建',
    to_status TINYINT UNSIGNED NOT NULL COMMENT '变化后状态',
    source_type TINYINT UNSIGNED NOT NULL COMMENT '1用户 2系统 3支付结果',
    reason_code VARCHAR(64) NOT NULL COMMENT '状态变更原因编码',
    source_event_id VARCHAR(64) DEFAULT NULL COMMENT '上游支付/消息事件ID（逻辑关联）',
    occurred_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_trade_sub_order_status_log_sub (sub_order_id, occurred_at, id),
    KEY idx_trade_sub_order_status_log_event (source_event_id),
    CONSTRAINT fk_trade_sub_order_status_log_sub FOREIGN KEY (sub_order_id)
        REFERENCES trade_sub_order(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_trade_sub_order_status_log_from CHECK (
        from_status IS NULL OR from_status IN (0, 10, 20, 30, 40)
    ),
    CONSTRAINT ck_trade_sub_order_status_log_to CHECK (to_status IN (0, 10, 20, 30, 40)),
    CONSTRAINT ck_trade_sub_order_status_log_changed CHECK (
        from_status IS NULL OR from_status <> to_status
    ),
    CONSTRAINT ck_trade_sub_order_status_log_source CHECK (source_type IN (1, 2, 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='商家子订单状态流转历史（追加写）';
