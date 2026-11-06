-- 差异处置审计流水：记录发起修复、关联补偿、复核通过或人工忽略等动作。
-- IAM 管理员账号ID仅逻辑引用，不建跨库外键。
CREATE TABLE reliability_reconciliation_resolution_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '差异处置流水ID',
    action_no VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '处置动作唯一编号',
    difference_id BIGINT UNSIGNED NOT NULL COMMENT '对应本地差异ID',
    action_code VARCHAR(64) NOT NULL COMMENT 'CREATE_RECOVERY / RECHECK / MARK_RESOLVED / IGNORE 等',
    recovery_task_id BIGINT UNSIGNED DEFAULT NULL COMMENT '本次涉及的本地恢复任务，可为空',
    from_status TINYINT UNSIGNED DEFAULT NULL COMMENT '处置前差异状态（纯备注动作可为空）',
    to_status TINYINT UNSIGNED DEFAULT NULL COMMENT '处置后差异状态（纯备注动作可为空）',
    actor_kind TINYINT UNSIGNED NOT NULL COMMENT '1系统 2管理员',
    actor_account_id BIGINT UNSIGNED DEFAULT NULL COMMENT 'IAM管理员账号ID，系统动作为空',
    note_summary VARCHAR(1000) DEFAULT NULL COMMENT '脱敏处置/复核记录',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '追踪ID',
    occurred_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_reliability_resolution_action_no (action_no),
    KEY idx_reliability_resolution_difference_time (difference_id, occurred_at, id),
    KEY idx_reliability_resolution_task (recovery_task_id),
    KEY idx_reliability_resolution_actor (actor_account_id, occurred_at),

    CONSTRAINT ck_reliability_resolution_from CHECK
        (from_status IS NULL OR from_status IN (0, 10, 20, 30)),
    CONSTRAINT ck_reliability_resolution_to CHECK
        (to_status IS NULL OR to_status IN (0, 10, 20, 30)),
    CONSTRAINT ck_reliability_resolution_status_pair CHECK
        ((from_status IS NULL AND to_status IS NULL)
         OR (from_status IS NOT NULL AND to_status IS NOT NULL)),
    CONSTRAINT ck_reliability_resolution_actor CHECK
        ((actor_kind = 1 AND actor_account_id IS NULL)
         OR (actor_kind = 2 AND actor_account_id IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='对账差异的人工/自动处理日志，保留恢复过程与复核依据';
