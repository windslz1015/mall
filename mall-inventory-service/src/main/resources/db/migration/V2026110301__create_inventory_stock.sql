-- 一个可交易 SKU 对应一条库存余额；库存最小单位为 SKU，不属于 product-service。
-- SKU/商品/商家 ID 是跨微服务逻辑引用，不建立跨库外键。
-- on_hand_qty = available_qty + reserved_qty，支付确认时扣 on_hand 和 reserved。
CREATE TABLE inventory_stock (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库存余额记录ID',
    sku_id BIGINT UNSIGNED NOT NULL COMMENT 'product-service SKU主键，逻辑引用',
    product_id BIGINT UNSIGNED NOT NULL COMMENT 'product-service SPU主键，逻辑引用',
    merchant_id BIGINT UNSIGNED NOT NULL COMMENT 'merchant-service 商家主体ID，逻辑引用',
    on_hand_qty BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前实有库存数量（包含预占）',
    available_qty BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '可供新订单锁定的库存数量',
    reserved_qty BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已锁定但尚未确认扣减的库存数量',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '库存行乐观锁版本',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_stock_sku (sku_id),
    KEY idx_inventory_stock_merchant (merchant_id, id),
    KEY idx_inventory_stock_product (product_id, id),
    CONSTRAINT ck_inventory_stock_balance CHECK (on_hand_qty = available_qty + reserved_qty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='SKU 库存余额，实有 = 可用 + 预占';
