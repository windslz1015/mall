-- 这里只保存对象存储文件 Key，不存 OSS 访问凭证与图像二进制。
-- image_type: 1 SPU封面、2 SPU图集/详情图、3 SKU专属图片。
-- cover_slot 利用 NULL 不参与唯一冲突的特性，保证每个商品至多一张封面。
CREATE TABLE product_image (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '图片记录ID',
    product_id BIGINT UNSIGNED NOT NULL COMMENT '商品SPU ID',
    sku_id BIGINT UNSIGNED DEFAULT NULL COMMENT 'SKU ID，图片类型为3时必填',
    object_key VARCHAR(512) NOT NULL COMMENT 'OSS/MinIO对象存储Key',
    image_type TINYINT UNSIGNED NOT NULL COMMENT '1封面 2图集/详情 3SKU图',
    alt_text VARCHAR(200) DEFAULT NULL COMMENT '图片说明',
    sort_no INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '展示排序',
    cover_slot TINYINT GENERATED ALWAYS AS
        (CASE WHEN image_type = 1 THEN 1 ELSE NULL END) STORED,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_image_one_cover (product_id, cover_slot),
    KEY idx_product_image_product_type_sort (product_id, image_type, sort_no, id),
    KEY idx_product_image_sku_product (sku_id, product_id),
    CONSTRAINT fk_product_image_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_product_image_sku_product
        FOREIGN KEY (sku_id, product_id) REFERENCES product_sku(id, product_id),
    CONSTRAINT ck_product_image_type CHECK (image_type IN (1, 2, 3)),
    CONSTRAINT ck_product_image_type_sku CHECK (
        (image_type = 3 AND sku_id IS NOT NULL)
        OR (image_type IN (1, 2) AND sku_id IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='商品及SKU图片引用';
