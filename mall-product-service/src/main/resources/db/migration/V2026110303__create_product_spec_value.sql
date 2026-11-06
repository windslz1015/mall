-- 规格值，例如颜色=黑色/白色，容量=128G/256G。
CREATE TABLE product_spec_value (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '规格值ID',
    spec_id BIGINT UNSIGNED NOT NULL COMMENT '所属规格项ID',
    value_name VARCHAR(128) NOT NULL COMMENT '规格值，如黑色、128G',
    sort_no INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '展示排序',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_spec_value_name (spec_id, value_name),
    UNIQUE KEY uk_product_spec_value_id_spec (id, spec_id),
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='商品规格值（如黑色）';
