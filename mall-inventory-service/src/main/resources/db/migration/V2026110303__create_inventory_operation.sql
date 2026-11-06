-- 每次成功的单SKU变更记录一条操作；与余额、预占、流水同一个本地事务提交。
-- 失败回滚则不留成功幂等记录；业务重试必须校验同一个 key 的请求指纹。
CREATE TABLE inventory_operation (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库存操作记录ID',
    operation_no VARCHAR(64) NOT NULL COMMENT '库存操作业务编号，全局唯一',
    operation_type TINYINT UNSIGNED NOT NULL COMMENT '1调整 2锁定 3确认扣减 4释放',
    source_service VARCHAR(64) NOT NULL COMMENT '操作请求来源服务（如 order-service）',
    request_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT '业务幂等键，区分大小写',
    request_fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT '规范化请求参数的SHA-256指纹',
    inventory_id BIGINT UNSIGNED NOT NULL COMMENT '被修改的本服务库存余额ID',
    reservation_id BIGINT UNSIGNED DEFAULT NULL COMMENT '预占记录ID，调整库存时为空',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_operation_no (operation_no),
    UNIQUE KEY uk_inventory_operation_request (source_service, operation_type, request_key),
    UNIQUE KEY uk_inventory_operation_id_stock (id, inventory_id),
    UNIQUE KEY uk_inventory_operation_id_reservation (id, reservation_id),
    KEY idx_inventory_operation_stock_time (inventory_id, created_at, id),
    KEY idx_inventory_operation_reservation (reservation_id, id),
    CONSTRAINT ck_inventory_operation_type CHECK (operation_type IN (1, 2, 3, 4)),
    CONSTRAINT ck_inventory_operation_reservation_type CHECK (
        (operation_type = 1 AND reservation_id IS NULL)
        OR (operation_type IN (2, 3, 4) AND reservation_id IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='库存幂等操作；一条记录只对应一次成功的单SKU库存变更';
