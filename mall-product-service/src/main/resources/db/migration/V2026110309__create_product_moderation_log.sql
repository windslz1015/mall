-- 平台强制下架的专项监管记录。和状态变更日志关联，不能直接代替商品当前状态。
-- 只允许商品服务按业务规则在一个本地事务中写入强制下架结果及两类日志。
CREATE TABLE product_moderation_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商品监管记录ID',
    product_id BIGINT UNSIGNED NOT NULL COMMENT '商品SPU ID',
    status_log_id BIGINT UNSIGNED NOT NULL COMMENT '对应的强制下架状态变更记录',
    action_type TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1平台强制下架',
    admin_account_id BIGINT UNSIGNED NOT NULL COMMENT '管理员账号ID（IAM）',
    reason VARCHAR(500) NOT NULL COMMENT '强制下架原因',
    evidence_object_key VARCHAR(512) DEFAULT NULL COMMENT '监管证据文件OSS Key（可选）',
    handled_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '处理时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_moderation_status_log (status_log_id),
    KEY idx_product_moderation_product_time (product_id, handled_at, id),
    KEY idx_product_moderation_admin_time (admin_account_id, handled_at),
    CONSTRAINT fk_product_moderation_status_product
        FOREIGN KEY (status_log_id, product_id) REFERENCES product_status_log(id, product_id),
    CONSTRAINT ck_product_moderation_action CHECK (action_type = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='平台商品强制下架监管记录';
