-- 一个 SPU 可以有多个 SKU，SKU 是交易与库存的最小规格单位。
-- 当前成交报价存储于 SKU；实际订单成交快照归 order-service，库存归 inventory-service。
-- spec_signature 为按规格项ID排序后的规格值组合做 SHA-256（小写64位十六进制），
-- 无规格的默认 SKU 对空组合计算同一签名；防止一个商品出现重复规格组合。
CREATE TABLE product_sku (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'SKU主键',
    sku_no VARCHAR(32) NOT NULL COMMENT 'SKU业务编号（全局唯一）',
    product_id BIGINT UNSIGNED NOT NULL COMMENT '所属商品SPU',
    merchant_sku_code VARCHAR(64) DEFAULT NULL COMMENT '商家自定义SKU编码（当前SPU内唯一）',
    sku_name VARCHAR(200) DEFAULT NULL COMMENT 'SKU展示名称',
    spec_signature CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'SKU规格组合的SHA-256指纹',
    sale_price DECIMAL(18,2) NOT NULL COMMENT '当前销售单价',
    currency CHAR(3) NOT NULL DEFAULT 'CNY' COMMENT '计价币种，当前业务建议CNY',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1可选 2停用',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_sku_no (sku_no),
    UNIQUE KEY uk_product_sku_product_signature (product_id, spec_signature),
    UNIQUE KEY uk_product_sku_merchant_code (product_id, merchant_sku_code),
    UNIQUE KEY uk_product_sku_id_product (id, product_id),
    KEY idx_product_sku_product_status (product_id, status, id),
    CONSTRAINT ck_product_sku_status CHECK (status IN (1, 2)),
    CONSTRAINT ck_product_sku_price CHECK (sale_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='商品规格SKU与当前销售价';
