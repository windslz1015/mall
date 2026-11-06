-- 当前价格以 product_sku.sale_price 为准；历史成交价以订单项快照为准。
-- 每次修改销售价格时，与更新 SKU 价格在同一商品库本地事务中追加一条日志。
CREATE TABLE product_price_change_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '价格变更流水ID',
    product_id BIGINT UNSIGNED NOT NULL COMMENT '所属商品SPU',
    sku_id BIGINT UNSIGNED NOT NULL COMMENT '被修改SKU',
    old_price DECIMAL(18,2) NOT NULL COMMENT '原销售价',
    new_price DECIMAL(18,2) NOT NULL COMMENT '新销售价',
    currency CHAR(3) NOT NULL DEFAULT 'CNY' COMMENT '价格币种',
    operator_account_id BIGINT UNSIGNED NOT NULL COMMENT '操作账号ID（IAM）',
    change_reason VARCHAR(500) DEFAULT NULL COMMENT '变价原因',
    changed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '变价时间',
    PRIMARY KEY (id),
    KEY idx_product_price_sku_time (sku_id, changed_at, id),
    KEY idx_product_price_product_time (product_id, changed_at, id),

    CONSTRAINT ck_product_price_old CHECK (old_price >= 0),
    CONSTRAINT ck_product_price_new CHECK (new_price >= 0),
    CONSTRAINT ck_product_price_diff CHECK (old_price <> new_price)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='SKU销售价格变更历史';
