-- 记录工单的分派、排查、沟通与状态变更；不存原始支付回调或其他敏感数据。
CREATE TABLE admin_investigation_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '排查日志主键',
    investigation_id BIGINT UNSIGNED NOT NULL COMMENT '异常排查工单ID',
    event_code VARCHAR(64) NOT NULL COMMENT '事件类型：OPEN/ASSIGN/NOTE/RESOLVE/CLOSE等',
    from_status TINYINT UNSIGNED DEFAULT NULL COMMENT '原排查状态（非状态事件允许为空）',
    to_status TINYINT UNSIGNED DEFAULT NULL COMMENT '新排查状态（非状态事件允许为空）',
    actor_kind TINYINT UNSIGNED NOT NULL COMMENT '1管理员 2系统',
    actor_account_id BIGINT UNSIGNED DEFAULT NULL COMMENT 'IAM管理员账号ID；系统事件为空',
    note_summary VARCHAR(1000) DEFAULT NULL COMMENT '脱敏排查记录',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '日志来源链路ID',
    occurred_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_admin_investigation_log_inv_time (investigation_id, occurred_at, id),
    KEY idx_admin_investigation_log_actor (actor_account_id, occurred_at),
    CONSTRAINT fk_admin_investigation_log_inv FOREIGN KEY (investigation_id)
        REFERENCES admin_exception_investigation (id),
    CONSTRAINT ck_admin_investigation_log_from CHECK
        (from_status IS NULL OR from_status IN (0, 10, 20, 30)),
    CONSTRAINT ck_admin_investigation_log_to CHECK
        (to_status IS NULL OR to_status IN (0, 10, 20, 30)),
    CONSTRAINT ck_admin_investigation_log_pair CHECK
        ((from_status IS NULL AND to_status IS NULL)
         OR (from_status IS NOT NULL AND to_status IS NOT NULL)),
    CONSTRAINT ck_admin_investigation_log_actor CHECK
        ((actor_kind = 1 AND actor_account_id IS NOT NULL)
         OR (actor_kind = 2 AND actor_account_id IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='平台异常排查工单事件与状态变更历史';
