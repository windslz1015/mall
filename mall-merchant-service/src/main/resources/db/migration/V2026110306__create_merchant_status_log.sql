-- 禁用/启用记录只追加不覆盖，与 merchant 当前状态相互区分。
CREATE TABLE merchant_status_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '状态流水ID',
    merchant_id BIGINT UNSIGNED NOT NULL COMMENT '商家主体ID',
    operator_account_id BIGINT UNSIGNED NOT NULL COMMENT '操作管理员账号ID（IAM）',
    previous_status TINYINT UNSIGNED NOT NULL COMMENT '变更前状态：1正常 2禁用',
    new_status TINYINT UNSIGNED NOT NULL COMMENT '变更后状态：1正常 2禁用',
    change_reason VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
    occurred_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '变更时间',
    PRIMARY KEY (id),
    KEY idx_merchant_status_log_merchant_time (merchant_id, occurred_at, id),
    KEY idx_merchant_status_log_operator_time (operator_account_id, occurred_at),
    CONSTRAINT ck_merchant_status_log_previous CHECK (previous_status IN (1, 2)),
    CONSTRAINT ck_merchant_status_log_new CHECK (new_status IN (1, 2)),
    CONSTRAINT ck_merchant_status_log_changed CHECK (previous_status <> new_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='商家经营状态变更流水表';
