-- SKU 选择的规格组合：一个 SKU 的每个规格项只能对应一个规格值。
-- 冗余 product_id 用于组合外键，数据库可直接阻止跨商品的 SKU-规格错误引用。
CREATE TABLE product_sku_spec_value (
    sku_id BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    product_id BIGINT UNSIGNED NOT NULL COMMENT '所属SPU ID（供组合外键校验）',
    spec_id BIGINT UNSIGNED NOT NULL COMMENT '规格项ID',
    spec_value_id BIGINT UNSIGNED NOT NULL COMMENT '规格值ID',
    PRIMARY KEY (sku_id, spec_id),
    KEY idx_sku_spec_spec_product (spec_id, product_id),
    KEY idx_sku_spec_value_spec (spec_value_id, spec_id),
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='SKU选定规格值关系';
