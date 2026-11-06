-- 按订单明细 item_id 唯一预占库存；重复下单请求不允许产生第二个有效预占。
-- 历史预占保留，不进行物理删除；确认与释放只能从 LOCKED 转移。
CREATE TABLE inventory_reservation (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库存预占记录ID',
    reservation_no VARCHAR(64) NOT NULL COMMENT '库存预占业务编号，全局唯一',
    inventory_id BIGINT UNSIGNED NOT NULL COMMENT '本服务库存余额ID',
    order_id BIGINT UNSIGNED NOT NULL COMMENT 'order-service 交易主订单ID，逻辑引用',
    sub_order_id BIGINT UNSIGNED NOT NULL COMMENT 'order-service 商家子订单ID，逻辑引用',
    order_item_id BIGINT UNSIGNED NOT NULL COMMENT 'order-service 订单明细ID，逻辑引用',
    quantity BIGINT UNSIGNED NOT NULL COMMENT '该订单明细预占的SKU数量',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1锁定 2已确认扣减 3已释放',
    expires_at DATETIME(3) NOT NULL COMMENT '业务预占到期时间；不代表数据库会自动释放',
    locked_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    confirmed_at DATETIME(3) DEFAULT NULL,
    released_at DATETIME(3) DEFAULT NULL,
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态乐观锁版本',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_reservation_no (reservation_no),
    UNIQUE KEY uk_inventory_reservation_order_item (order_item_id),
    UNIQUE KEY uk_inventory_reservation_id_stock (id, inventory_id),
    KEY idx_inventory_reservation_order (order_id, status, id),
    KEY idx_inventory_reservation_sub_order (sub_order_id, id),
    KEY idx_inventory_reservation_stock_status (inventory_id, status, id),
    KEY idx_inventory_reservation_expiry (status, expires_at, id),
    CONSTRAINT ck_inventory_reservation_quantity CHECK (quantity > 0),
    CONSTRAINT ck_inventory_reservation_status CHECK (status IN (1, 2, 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='订单明细SKU库存预占及确认/释放状态';
