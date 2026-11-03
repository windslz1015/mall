-- 订单项：保存成交时的商品、规格、价格与图片Key的不可变快照。
-- product_id/sku_id 对应 product-service；商家归属由对应子订单确定。
-- 一个子订单中的同一SKU聚合为一条订单项（当前无优惠拆行场景）。
-- inventory-service.inventory_reservation.order_item_id 逻辑引用本表ID。
CREATE TABLE trade_order_item (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '交易订单项ID',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '所属交易主订单ID',
    sub_order_id BIGINT UNSIGNED NOT NULL COMMENT '所属商家子订单ID',
    product_id BIGINT UNSIGNED NOT NULL COMMENT 'product-service商品SPU ID（逻辑引用）',
    sku_id BIGINT UNSIGNED NOT NULL COMMENT 'product-service商品SKU ID（逻辑引用）',
    product_name_snapshot VARCHAR(200) NOT NULL COMMENT '下单时商品名称快照',
    sku_no_snapshot VARCHAR(32) NOT NULL COMMENT '下单时SKU业务编号快照',
    sku_name_snapshot VARCHAR(200) DEFAULT NULL COMMENT '下单时SKU展示名称快照',
    spec_snapshot JSON NOT NULL COMMENT '下单时规格快照，空规格传空数组[]',
    cover_object_key_snapshot VARCHAR(512) DEFAULT NULL COMMENT '下单时商品主图OSS对象Key快照',
    unit_price DECIMAL(18,2) NOT NULL COMMENT '成交单价，不随商品改价变化',
    quantity INT UNSIGNED NOT NULL COMMENT '购买数量，大于0',
    line_amount DECIMAL(18,2) NOT NULL COMMENT '订单项金额 = 单价 * 数量',
    currency CHAR(3) NOT NULL DEFAULT 'CNY',
    snapshot_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '成交快照形成时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_trade_order_item_sub_sku (sub_order_id, sku_id),
    KEY idx_trade_order_item_parent (order_id, sub_order_id, id),
    KEY idx_trade_order_item_sku (sku_id, created_at, id),
    CONSTRAINT fk_trade_order_item_parent_sub
        FOREIGN KEY (sub_order_id, order_id)
        REFERENCES trade_sub_order(id, order_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_trade_order_item_quantity CHECK (quantity > 0),
    CONSTRAINT ck_trade_order_item_money CHECK (
        unit_price >= 0 AND line_amount >= 0
        AND line_amount = unit_price * quantity
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='订单商品明细及不可变交易快照';
