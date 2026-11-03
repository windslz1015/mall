-- 入驻申请审核的不可覆盖历史，供审核审计与异常排查使用。
CREATE TABLE merchant_review_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '审核记录ID',
    application_id BIGINT UNSIGNED NOT NULL COMMENT '入驻申请ID',
    reviewer_account_id BIGINT UNSIGNED NOT NULL COMMENT '管理员账号ID（IAM）',
    previous_status TINYINT UNSIGNED NOT NULL COMMENT '审核前状态',
    new_status TINYINT UNSIGNED NOT NULL COMMENT '审核后状态：2通过 3拒绝',
    review_remark VARCHAR(500) DEFAULT NULL COMMENT '审核说明',
    reviewed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '审核时间',
    PRIMARY KEY (id),
    KEY idx_merchant_review_application_time (application_id, reviewed_at, id),
    KEY idx_merchant_review_operator_time (reviewer_account_id, reviewed_at),
    CONSTRAINT fk_merchant_review_application
        FOREIGN KEY (application_id) REFERENCES merchant_application(id),
    CONSTRAINT ck_merchant_review_previous_status CHECK (previous_status IN (1, 2, 3)),
    CONSTRAINT ck_merchant_review_new_status CHECK (new_status IN (2, 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='入驻审核历史记录表';
