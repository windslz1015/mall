-- 不可变库存流水：记录每次库存动作前后完整数量，便于对账、审计与故障排查。
-- 库存动作对应一个 operation_id；具体类型可通过 inventory_operation.operation_type 查询。
CREATE TABLE inventory_stock_ledger (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库存数量变更流水ID',
    operation_id BIGINT UNSIGNED NOT NULL COMMENT '对应库存幂等操作ID（唯一）',
    inventory_id BIGINT UNSIGNED NOT NULL COMMENT '被修改的库存余额记录ID',
    reservation_id BIGINT UNSIGNED DEFAULT NULL COMMENT '对应预占记录，库存调整时为空',
    on_hand_before BIGINT UNSIGNED NOT NULL COMMENT '变更前实有库存',
    available_before BIGINT UNSIGNED NOT NULL COMMENT '变更前可用库存',
    reserved_before BIGINT UNSIGNED NOT NULL COMMENT '变更前已锁定库存',
    on_hand_after BIGINT UNSIGNED NOT NULL COMMENT '变更后实有库存',
    available_after BIGINT UNSIGNED NOT NULL COMMENT '变更后可用库存',
    reserved_after BIGINT UNSIGNED NOT NULL COMMENT '变更后已锁定库存',
    on_hand_delta BIGINT NOT NULL COMMENT '实有库存变化量，允许负值',
    available_delta BIGINT NOT NULL COMMENT '可用库存变化量，允许负值',
    reserved_delta BIGINT NOT NULL COMMENT '锁定库存变化量，允许负值',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_stock_ledger_operation (operation_id),
    KEY idx_inventory_stock_ledger_stock_time (inventory_id, created_at, id),
    KEY idx_inventory_stock_ledger_reservation_time (reservation_id, created_at, id),
    CONSTRAINT fk_inventory_stock_ledger_operation_stock
        FOREIGN KEY (operation_id, inventory_id)
        REFERENCES inventory_operation(id, inventory_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_inventory_stock_ledger_reservation_stock
        FOREIGN KEY (reservation_id, inventory_id)
        REFERENCES inventory_reservation(id, inventory_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_inventory_stock_ledger_before CHECK
        (on_hand_before = available_before + reserved_before),
    CONSTRAINT ck_inventory_stock_ledger_after CHECK
        (on_hand_after = available_after + reserved_after)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='每次库存变动前后快照与增量，永久保留供追溯';
