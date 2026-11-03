-- 一个 SPU 拥有多组规格，例如颜色、容量；规格值在下一张表单独维护。
CREATE TABLE product_spec (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '规格项ID',
    product_id BIGINT UNSIGNED NOT NULL COMMENT '所属SPU',
    spec_name VARCHAR(64) NOT NULL COMMENT '规格名称，如颜色、容量',
    sort_no INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '展示排序',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_spec_name (product_id, spec_name),
    UNIQUE KEY uk_product_spec_id_product (id, product_id),
    CONSTRAINT fk_product_spec_product FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='商品规格项（如颜色）';
