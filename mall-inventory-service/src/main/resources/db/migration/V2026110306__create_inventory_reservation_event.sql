-- 预占建立、确认、释放的状态变化历史（库存锁定/确认/释放的操作必须有对应事件）。
-- 本表不承载事件消息投递；RabbitMQ Outbox 应在后续明确异步业务场景后单独设计。
CREATE TABLE inventory_reservation_event (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '预占状态事件ID',
    reservation_id BIGINT UNSIGNED NOT NULL COMMENT '库存预占记录ID',
    operation_id BIGINT UNSIGNED NOT NULL COMMENT '导致本次状态变化的库存操作ID',
    from_status TINYINT UNSIGNED DEFAULT NULL COMMENT '变更前状态；NULL表示初次锁定',
    to_status TINYINT UNSIGNED NOT NULL COMMENT '1锁定 2确认扣减 3释放',
    reason_code VARCHAR(64) DEFAULT NULL COMMENT '释放原因（取消/超时/异常等），可选',
    occurred_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_reservation_event_operation (operation_id),
    KEY idx_inventory_reservation_event_reservation (reservation_id, occurred_at, id),
    CONSTRAINT ck_inventory_reservation_event_transition CHECK (
        (from_status IS NULL AND to_status = 1)
        OR (from_status = 1 AND to_status IN (2, 3))
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='库存预占状态变化历史，初次锁定及最终确认/释放';
