-- 商品 SPU：归属 merchant-service 的经营主体，不保存商家账号/认证信息。
-- merchant_id、IAM 的 account_id 均为跨服务逻辑引用，不做跨库外键。
-- 0草稿 1上架 2商家下架 3平台强制下架。
CREATE TABLE product (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商品SPU主键',
    product_no VARCHAR(32) NOT NULL COMMENT '商品业务编号（全局唯一）',
    merchant_id BIGINT UNSIGNED NOT NULL COMMENT '经营商家主体ID（merchant-service）',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    subtitle VARCHAR(255) DEFAULT NULL COMMENT '商品副标题',
    description TEXT DEFAULT NULL COMMENT '商品详情文本，不存图片文件',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0草稿 1上架 2下架 3平台强制下架',
    last_published_at DATETIME(3) DEFAULT NULL COMMENT '最近一次上架时间',
    last_off_shelf_at DATETIME(3) DEFAULT NULL COMMENT '最近一次下架时间',
    created_by_account_id BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人账号ID（IAM）',
    updated_by_account_id BIGINT UNSIGNED DEFAULT NULL COMMENT '最后操作人账号ID（IAM）',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_no (product_no),
    KEY idx_product_merchant_status_created (merchant_id, status, created_at, id),
    KEY idx_product_status_published (status, last_published_at, id),
    CONSTRAINT ck_product_status CHECK (status IN (0, 1, 2, 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='商品基础信息(SPU)';
