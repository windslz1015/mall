-- 初始化/补货/盘点减量均视为库存调整；不能直接无流水修改库存。
-- 具体允许的调整原因及权限在未来领域服务中实现。
CREATE TABLE inventory_adjustment (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库存调整审计记录ID',
    operation_id BIGINT UNSIGNED NOT NULL COMMENT '对应成功的库存调整操作',
    inventory_id BIGINT UNSIGNED NOT NULL COMMENT '被调整的库存记录',
    quantity_delta BIGINT NOT NULL COMMENT '带符号的实有和可用库存调整增量',
    reason_code VARCHAR(64) NOT NULL COMMENT '业务调整原因编码',
    reason_text VARCHAR(500) NOT NULL COMMENT '调整原因说明',
    operator_type TINYINT UNSIGNED NOT NULL COMMENT '1商家 2平台管理员 3系统',
    operator_account_id BIGINT UNSIGNED DEFAULT NULL COMMENT 'IAM 操作账号ID，系统操作时为空',
    external_ref VARCHAR(128) DEFAULT NULL COMMENT '外部业务凭据/盘点批次编号（可选）',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_adjustment_operation (operation_id),
    KEY idx_inventory_adjustment_stock_time (inventory_id, created_at, id),
    KEY idx_inventory_adjustment_operator (operator_type, operator_account_id, created_at),
    CONSTRAINT fk_inventory_adjustment_operation_stock
        FOREIGN KEY (operation_id, inventory_id)
        REFERENCES inventory_operation(id, inventory_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_inventory_adjustment_delta CHECK (quantity_delta <> 0),
    CONSTRAINT ck_inventory_adjustment_operator_type CHECK (operator_type IN (1, 2, 3)),
    CONSTRAINT ck_inventory_adjustment_operator CHECK (
        (operator_type = 3 AND operator_account_id IS NULL)
        OR (operator_type IN (1, 2) AND operator_account_id IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='人工或系统库存调整原因与操作审计';
