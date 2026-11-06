-- 审核通过后建立商家主体；与申请、商家账号均为不同业务对象。
CREATE TABLE merchant (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商家主体ID',
    merchant_no VARCHAR(32) NOT NULL COMMENT '平台商家业务编号',
    source_application_id BIGINT UNSIGNED NOT NULL COMMENT '来源入驻申请ID',
    merchant_name VARCHAR(128) NOT NULL COMMENT '商家主体名称',
    business_license_no VARCHAR(64) DEFAULT NULL COMMENT '营业执照号/主体登记号码',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1正常经营 2已禁用',
    approved_at DATETIME(3) NOT NULL COMMENT '商家审核通过时间',
    disabled_at DATETIME(3) DEFAULT NULL COMMENT '最近一次禁用时间',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_merchant_no (merchant_no),
    UNIQUE KEY uk_merchant_source_application (source_application_id),
    KEY idx_merchant_status_created (status, created_at, id),
    CONSTRAINT ck_merchant_status CHECK (status IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='商家经营主体表';
