-- 上下架/强制下架状态变更历史；仅追加，不反向覆盖。
-- operator_type: 1商家账号 2平台管理员 3系统自动任务；系统自动任务可无账号。
CREATE TABLE product_status_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '状态变更流水ID',
    product_id BIGINT UNSIGNED NOT NULL COMMENT '商品SPU ID',
    previous_status TINYINT UNSIGNED NOT NULL COMMENT '变更前状态',
    new_status TINYINT UNSIGNED NOT NULL COMMENT '变更后状态',
    operator_type TINYINT UNSIGNED NOT NULL COMMENT '1商家 2管理员 3系统',
    operator_account_id BIGINT UNSIGNED DEFAULT NULL COMMENT '操作账号ID（IAM），系统可为空',
    change_reason VARCHAR(500) DEFAULT NULL COMMENT '操作原因',
    occurred_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '变更时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_status_log_id_product (id, product_id),
    KEY idx_product_status_log_product_time (product_id, occurred_at, id),
    KEY idx_product_status_log_operator (operator_account_id, occurred_at),
    CONSTRAINT fk_product_status_log_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT ck_product_status_log_old CHECK (previous_status IN (0, 1, 2, 3)),
    CONSTRAINT ck_product_status_log_new CHECK (new_status IN (0, 1, 2, 3)),
    CONSTRAINT ck_product_status_log_diff CHECK (previous_status <> new_status),
    CONSTRAINT ck_product_status_log_operator_type CHECK (operator_type IN (1, 2, 3)),
    CONSTRAINT ck_product_status_log_operator_present CHECK (
        (operator_type = 3 AND operator_account_id IS NULL)
        OR (operator_type IN (1, 2) AND operator_account_id IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='商品生命周期状态变更流水';
