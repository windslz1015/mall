-- 商家入驻申请是独立业务对象，不是商家主体本身。
-- applicant_account_id / reviewer_account_id 属于 IAM 服务，不建立跨库外键。
CREATE TABLE merchant_application (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '入驻申请ID',
    application_no VARCHAR(32) NOT NULL COMMENT '入驻申请业务编号',
    applicant_account_id BIGINT UNSIGNED NOT NULL COMMENT '申请账号ID（IAM）',
    proposed_merchant_name VARCHAR(128) NOT NULL COMMENT '申请的商家名称',
    business_license_no VARCHAR(64) DEFAULT NULL COMMENT '营业执照号/主体登记号码（可选）',
    contact_name VARCHAR(64) NOT NULL COMMENT '申请联系人',
    contact_phone VARCHAR(32) NOT NULL COMMENT '申请联系电话',
    contact_email VARCHAR(128) DEFAULT NULL COMMENT '联系邮箱',
    business_description VARCHAR(1000) DEFAULT NULL COMMENT '经营情况说明',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1待审核 2审核通过 3审核拒绝',
    reviewer_account_id BIGINT UNSIGNED DEFAULT NULL COMMENT '审核管理员账号ID（IAM）',
    review_remark VARCHAR(500) DEFAULT NULL COMMENT '最近一次审核说明',
    submitted_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '提交时间',
    reviewed_at DATETIME(3) DEFAULT NULL COMMENT '审核时间',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_merchant_application_no (application_no),
    KEY idx_merchant_apply_account_time (applicant_account_id, submitted_at),
    KEY idx_merchant_apply_status_time (status, submitted_at, id),
    KEY idx_merchant_apply_license (business_license_no),
    CONSTRAINT ck_merchant_apply_status CHECK (status IN (1, 2, 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='商家入驻申请表';
