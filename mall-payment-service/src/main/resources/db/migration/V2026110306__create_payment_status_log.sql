-- 支付单状态变更的追加历史；不替代 payment_order 当前状态。
CREATE TABLE payment_status_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付单状态日志主键',
    payment_order_id BIGINT UNSIGNED NOT NULL COMMENT '所属支付单',
    operation_id BIGINT UNSIGNED DEFAULT NULL COMMENT '触发本次状态变更的幂等操作',
    from_status TINYINT UNSIGNED DEFAULT NULL COMMENT '变更前状态；首次创建可为空',
    to_status TINYINT UNSIGNED NOT NULL COMMENT '变更后状态',
    change_source VARCHAR(32) NOT NULL COMMENT 'USER/CHANNEL/ORDER/SYSTEM/RECONCILE',
    reason_code VARCHAR(64) DEFAULT NULL COMMENT '变更原因代码',
    remark VARCHAR(500) DEFAULT NULL COMMENT '脱敏变更说明',
    occurred_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_payment_status_log_order (payment_order_id, occurred_at, id),
    KEY idx_payment_status_log_operation (operation_id),
    CONSTRAINT fk_payment_status_log_order FOREIGN KEY (payment_order_id)
        REFERENCES payment_order (id),
    CONSTRAINT fk_payment_status_log_operation FOREIGN KEY (operation_id)
        REFERENCES payment_operation (id),
    CONSTRAINT ck_payment_status_log_from CHECK
        (from_status IS NULL OR from_status IN (0, 10, 20, 30, 40, 50)),
    CONSTRAINT ck_payment_status_log_to CHECK (to_status IN (0, 10, 20, 30, 40, 50))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='支付单状态变更历史（仅追加）';
