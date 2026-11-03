-- 只保存账号ID与商家ID的关联，不复制IAM密码、Token、账号状态。
-- 一个商家预留多个操作账号的关联能力，具体员工权限模型未在此实现。
CREATE TABLE merchant_account_binding (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    merchant_id BIGINT UNSIGNED NOT NULL COMMENT '商家主体ID',
    account_id BIGINT UNSIGNED NOT NULL COMMENT '关联账号ID（IAM）',
    binding_role TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1负责人 2运营人员（预留）',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1有效 2停用',
    bound_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '绑定时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_merchant_account_binding (merchant_id, account_id),
    KEY idx_merchant_account_binding_account (account_id, status, merchant_id),
    KEY idx_merchant_account_binding_merchant (merchant_id, status),
    CONSTRAINT fk_merchant_account_binding_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchant(id),
    CONSTRAINT ck_merchant_account_binding_role CHECK (binding_role IN (1, 2)),
    CONSTRAINT ck_merchant_account_binding_status CHECK (status IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='商家账号与经营主体关联表';
