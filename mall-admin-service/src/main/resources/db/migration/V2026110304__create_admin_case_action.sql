-- 管理服务记录“请求对某个监管对象执行什么动作、是否成功”，不修改业务域核心表。
-- action_no 将来可作为调用下游服务的幂等请求键。
-- (case_target_id, case_id) 组合外键确保处置动作的对象归属于同一监管案件。
CREATE TABLE admin_case_action (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '监管处置动作主键',
    action_no VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '全局唯一处置请求号（幂等键）',
    case_id BIGINT UNSIGNED NOT NULL COMMENT '监管案件ID',
    case_target_id BIGINT UNSIGNED NOT NULL COMMENT '被处置的案件关联对象ID',
    action_code VARCHAR(64) NOT NULL COMMENT 'USER_DISABLE / MERCHANT_DISABLE / PRODUCT_TAKEDOWN 等',
    operator_account_id BIGINT UNSIGNED NOT NULL COMMENT 'IAM发起处置的管理员账号ID',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0创建 10已请求 20成功 30失败 40撤销',
    reason_summary VARCHAR(500) DEFAULT NULL COMMENT '脱敏处置原因',
    remote_operation_no VARCHAR(128) DEFAULT NULL COMMENT '业务服务返回的操作引用号（如有）',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '跨服务追踪ID',
    failure_summary VARCHAR(500) DEFAULT NULL COMMENT '脱敏失败原因',
    requested_at DATETIME(3) DEFAULT NULL COMMENT '请求业务服务执行时间',
    completed_at DATETIME(3) DEFAULT NULL COMMENT '处置成功/失败/撤销时间',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '后续实现乐观锁版本',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_case_action_no (action_no),
    KEY idx_admin_case_action_case_status (case_id, status, created_at),
    KEY idx_admin_case_action_target_case (case_target_id, case_id),
    KEY idx_admin_case_action_operator (operator_account_id, created_at),
    CONSTRAINT fk_admin_case_action_target_case FOREIGN KEY (case_target_id, case_id)
        REFERENCES admin_case_target (id, case_id),
    CONSTRAINT ck_admin_case_action_status CHECK (status IN (0, 10, 20, 30, 40)),
    CONSTRAINT ck_admin_case_action_completed CHECK
        (status NOT IN (20, 30, 40) OR completed_at IS NOT NULL),
    CONSTRAINT ck_admin_case_action_requested CHECK
        (status NOT IN (10, 20, 30) OR requested_at IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='监管处置请求/执行结果：真实资源状态由目标业务域维护';
