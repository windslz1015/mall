-- 一个监管案件可同时涉及多个业务对象，外部资源ID不建立跨业务域外键。
-- target_role: 1主要对象 2关联对象。
CREATE TABLE admin_case_target (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '监管对象关联主键',
    case_id BIGINT UNSIGNED NOT NULL COMMENT '平台监管案件ID',
    target_domain VARCHAR(20) NOT NULL COMMENT '业务域：IAM/MERCHANT/PRODUCT/ORDER/PAYMENT',
    target_type VARCHAR(48) NOT NULL COMMENT '资源类型：USER_ACCOUNT/MERCHANT/PRODUCT_SPU等',
    target_id VARCHAR(64) NOT NULL COMMENT '资源ID（跨服务逻辑引用）',
    target_role TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1主要监管对象 2其他关联对象',
    target_display_no VARCHAR(64) DEFAULT NULL COMMENT '可选脱敏业务单号快照，不保存账号私密资料',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_case_target_dedup (case_id, target_domain, target_type, target_id),
    UNIQUE KEY uk_admin_case_target_id_case (id, case_id),
    KEY idx_admin_case_target_resource (target_domain, target_type, target_id),
    CONSTRAINT fk_admin_case_target_case FOREIGN KEY (case_id)
        REFERENCES admin_regulation_case (id),
    CONSTRAINT ck_admin_case_target_domain CHECK
        (target_domain IN ('IAM', 'MERCHANT', 'PRODUCT', 'ORDER', 'PAYMENT')),
    CONSTRAINT ck_admin_case_target_role CHECK (target_role IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='监管案件与多业务域对象的关联；数据库只约束本地案件ID';
