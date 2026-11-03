-- 管理端操作审计；管理员账号由 IAM 管理，operator_account_id 仅逻辑关联，不建跨库 FK。
-- 查询类敏感动作也可以记日志；日志内容只存最小必要的脱敏审计信息。
CREATE TABLE admin_operation_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '审计日志主键',
    operation_no VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT '本次管理操作唯一业务编号',
    operator_account_id BIGINT UNSIGNED NOT NULL COMMENT 'IAM平台管理员账号ID（逻辑引用）',
    action_code VARCHAR(64) NOT NULL COMMENT '操作编码：例如 USER_DISABLE/PRODUCT_TAKEDOWN/ORDER_VIEW',
    target_domain VARCHAR(20) DEFAULT NULL COMMENT '目标所属业务域；无具体目标的操作可为NULL',
    target_type VARCHAR(48) DEFAULT NULL COMMENT '目标业务对象类型，如 USER_ACCOUNT/PRODUCT_SPU',
    target_id VARCHAR(64) DEFAULT NULL COMMENT '目标业务对象ID（逻辑引用）',
    outcome TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0开始 10成功 20失败',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT 'Gateway链路追踪ID',
    client_ip VARCHAR(45) DEFAULT NULL COMMENT '脱敏或合规保留的来源IP',
    reason_summary VARCHAR(500) DEFAULT NULL COMMENT '已脱敏的操作原因摘要',
    error_summary VARCHAR(500) DEFAULT NULL COMMENT '已脱敏的失败摘要',
    occurred_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作发生时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_operation_no (operation_no),
    KEY idx_admin_operation_actor_time (operator_account_id, occurred_at, id),
    KEY idx_admin_operation_action_time (action_code, occurred_at, id),
    KEY idx_admin_operation_target (target_domain, target_type, target_id, occurred_at),
    KEY idx_admin_operation_trace (trace_id),
    KEY idx_admin_operation_outcome_time (outcome, occurred_at, id),
    CONSTRAINT ck_admin_operation_outcome CHECK (outcome IN (0, 10, 20)),
    CONSTRAINT ck_admin_operation_domain CHECK
        (target_domain IS NULL OR target_domain IN ('IAM', 'MERCHANT', 'PRODUCT', 'ORDER', 'PAYMENT', 'INVENTORY', 'ADMIN', 'RELIABILITY')),
    CONSTRAINT ck_admin_operation_target CHECK
        ((target_type IS NULL AND target_id IS NULL) OR (target_type IS NOT NULL AND target_id IS NOT NULL)),
    CONSTRAINT ck_admin_operation_target_domain CHECK
        (target_type IS NULL OR target_domain IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='后台管理员操作审计：不保存其他业务域资源的可变状态';
