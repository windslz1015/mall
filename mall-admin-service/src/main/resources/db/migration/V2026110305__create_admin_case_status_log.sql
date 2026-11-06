-- 案件状态日志：独立于下游资源状态日志，推荐追加写不覆盖历史。
-- actor_kind: 1管理员 2系统；不强制所有状态变更都必须有管理员账号。
CREATE TABLE admin_case_status_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '案件状态日志ID',
    case_id BIGINT UNSIGNED NOT NULL COMMENT '案件ID',
    from_status TINYINT UNSIGNED DEFAULT NULL COMMENT '原案件状态；创建时可以为NULL',
    to_status TINYINT UNSIGNED NOT NULL COMMENT '新案件状态',
    actor_kind TINYINT UNSIGNED NOT NULL COMMENT '1管理员 2系统',
    actor_account_id BIGINT UNSIGNED DEFAULT NULL COMMENT 'IAM操作管理员账号ID（系统事件为空）',
    reason_summary VARCHAR(500) DEFAULT NULL COMMENT '脱敏状态变更说明',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '追踪ID',
    occurred_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_admin_case_status_log_case (case_id, occurred_at, id),
    KEY idx_admin_case_status_log_actor (actor_account_id, occurred_at),

    CONSTRAINT ck_admin_case_status_log_from CHECK
        (from_status IS NULL OR from_status IN (0, 10, 20, 30, 40, 50)),
    CONSTRAINT ck_admin_case_status_log_to CHECK
        (to_status IN (0, 10, 20, 30, 40, 50)),
    CONSTRAINT ck_admin_case_status_log_actor CHECK
        ((actor_kind = 1 AND actor_account_id IS NOT NULL)
         OR (actor_kind = 2 AND actor_account_id IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='监管案件自身状态变更历史（不等于商品或商家状态日志）';
