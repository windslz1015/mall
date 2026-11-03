-- 保存每次补偿尝试，唯一键避免同一任务同一 attempt_no 重复记载。
-- 执行结果只代表调用结果，不能推断支付渠道最终资金事实。
CREATE TABLE reliability_recovery_attempt (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '补偿尝试ID',
    recovery_task_id BIGINT UNSIGNED NOT NULL COMMENT '本地补偿任务ID',
    attempt_no INT UNSIGNED NOT NULL COMMENT '从1开始的执行次数',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0开始 10成功 20失败 30超时',
    worker_id VARCHAR(128) DEFAULT NULL COMMENT '执行实例标识',
    request_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '发送给下游的本次请求关联ID',
    downstream_operation_no VARCHAR(128) DEFAULT NULL COMMENT '目标业务域返回的幂等操作号（如有）',
    error_code VARCHAR(64) DEFAULT NULL COMMENT '错误码',
    error_summary VARCHAR(500) DEFAULT NULL COMMENT '已脱敏的错误摘要',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
    started_at DATETIME(3) NOT NULL COMMENT '尝试开始时间',
    finished_at DATETIME(3) DEFAULT NULL COMMENT '尝试结束时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_reliability_attempt_task_no (recovery_task_id, attempt_no),
    UNIQUE KEY uk_reliability_attempt_request (request_id),
    KEY idx_reliability_attempt_task_time (recovery_task_id, started_at, id),
    KEY idx_reliability_attempt_status_time (status, started_at),
    CONSTRAINT fk_reliability_attempt_task FOREIGN KEY (recovery_task_id)
        REFERENCES reliability_recovery_task (id),
    CONSTRAINT ck_reliability_attempt_positive CHECK (attempt_no > 0),
    CONSTRAINT ck_reliability_attempt_status CHECK (status IN (0, 10, 20, 30)),
    CONSTRAINT ck_reliability_attempt_finished CHECK
        (status = 0 OR finished_at IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='补偿任务尝试执行流水；不存业务请求原始敏感报文';
