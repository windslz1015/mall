-- 每一次任务触发的执行记录；计划调度时 scheduled_slot_key 采用同一时间槽稳定键防重复触发。
-- 手动执行可以留 scheduled_slot_key=NULL，允许同一任务手动多次执行。
CREATE TABLE reliability_job_execution (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '任务执行ID',
    execution_no VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '一次执行的唯一编号',
    job_id BIGINT UNSIGNED NOT NULL COMMENT '本地任务定义ID',
    scheduled_slot_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT '计划触发槽位幂等键；手动触发为NULL',
    trigger_kind TINYINT UNSIGNED NOT NULL COMMENT '1定时调度 2人工触发',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0排队 10执行中 20成功 30失败 40超时 50跳过',
    scheduled_at DATETIME(3) NOT NULL COMMENT '计划触发时间',
    worker_id VARCHAR(128) DEFAULT NULL COMMENT '执行实例标识',
    lease_token VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT '执行租约令牌',
    lease_expires_at DATETIME(3) DEFAULT NULL COMMENT '执行租约有效期',
    started_at DATETIME(3) DEFAULT NULL COMMENT '实际开始时间',
    finished_at DATETIME(3) DEFAULT NULL COMMENT '实际结束时间',
    processed_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '扫描/处理记录数',
    succeeded_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '成功处理数',
    failed_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '失败处理数',
    error_summary VARCHAR(500) DEFAULT NULL COMMENT '脱敏错误摘要',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_reliability_execution_no (execution_no),
    UNIQUE KEY uk_reliability_execution_slot (job_id, scheduled_slot_key),
    KEY idx_reliability_execution_job_time (job_id, scheduled_at, id),
    KEY idx_reliability_execution_status_time (status, scheduled_at, id),
    KEY idx_reliability_execution_lease (status, lease_expires_at),

    CONSTRAINT ck_reliability_execution_trigger CHECK (trigger_kind IN (1, 2)),
    CONSTRAINT ck_reliability_execution_slot CHECK
        ((trigger_kind = 1 AND scheduled_slot_key IS NOT NULL)
         OR (trigger_kind = 2 AND scheduled_slot_key IS NULL)),
    CONSTRAINT ck_reliability_execution_status CHECK (status IN (0, 10, 20, 30, 40, 50)),
    CONSTRAINT ck_reliability_execution_finished CHECK
        (status NOT IN (20, 30, 40, 50) OR finished_at IS NOT NULL),
    CONSTRAINT ck_reliability_execution_counts CHECK (succeeded_count + failed_count <= processed_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='定时任务每次执行情况；支持时间槽去重和租约状态预留';
