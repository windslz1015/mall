-- 购物车项是购物车聚合内部实体：一个 cart 下同一 sku_id 最多一条。
-- sku_id 只关联 product-service 的 SKU；价格、名称、规格、商家归属与可售状态查询实时商品数据。
-- 购物车的商品数量不代表库存预占；只有交易服务的下单流程才能调用库存锁定。
CREATE TABLE cart_item (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '购物车项主键ID',
    cart_id BIGINT UNSIGNED NOT NULL COMMENT '购物车ID（本服务内部关联）',
    sku_id BIGINT UNSIGNED NOT NULL COMMENT '商品SKU主键ID（product-service，逻辑引用）',
    quantity INT UNSIGNED NOT NULL COMMENT '待购数量，必须大于0；数量为0时通过删除项实现',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '购物车项乐观锁版本（业务写入时递增）',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '加入购物车时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_item_cart_sku (cart_id, sku_id),
    KEY idx_cart_item_cart_updated (cart_id, updated_at, id),
    CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id)
        REFERENCES cart (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_cart_item_positive_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='购物车项：一个购物车内一个SKU一条记录';
