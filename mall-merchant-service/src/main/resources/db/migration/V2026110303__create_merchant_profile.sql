-- 商家可变展示/联系资料，和商家主体身份信息分开。
CREATE TABLE merchant_profile (
    merchant_id BIGINT UNSIGNED NOT NULL COMMENT '商家主体ID',
    display_name VARCHAR(128) NOT NULL COMMENT '店铺展示名称',
    logo_url VARCHAR(512) DEFAULT NULL COMMENT '店铺Logo文件地址/OSS Key',
    introduction VARCHAR(1000) DEFAULT NULL COMMENT '商家简介',
    contact_name VARCHAR(64) DEFAULT NULL COMMENT '商家联系人',
    contact_phone VARCHAR(32) DEFAULT NULL COMMENT '商家联系电话',
    contact_email VARCHAR(128) DEFAULT NULL COMMENT '联系邮箱',
    contact_address VARCHAR(512) DEFAULT NULL COMMENT '联系地址',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (merchant_id),
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='商家展示与联系资料表';
