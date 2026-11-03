-- 定时任务元数据，供未来调度器读取；建表不代表后台定时任务已启用。
-- 1 CRON；2 FIXED_DELAY。扫描超时订单必须调用 order-service，不直接操作订单库。
CREATE TABLE reliability_job_definition (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '任务定义ID',
    job_code VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '任务唯一编码',
    job_name VARCHAR(128) NOT NULL COMMENT '任务名称',
    job_category VARCHAR(32) NOT NULL COMMENT 'ORDER_TIMEOUT_SCAN / RETRY_SCAN / RECONCILIATION / CUSTOM',
    target_service VARCHAR(64) NOT NULL COMMENT '业务动作真正归属的服务',
    trigger_action VARCHAR(64) NOT NULL COMMENT '未来调用的幂等动作编码，非任意URL',
    schedule_mode TINYINT UNSIGNED NOT NULL COMMENT '1=CRON 2=FIXED_DELAY',
    cron_expression VARCHAR(96) DEFAULT NULL COMMENT '仅 CRON 模式有值；需应用层进一步验证',
    fixed_delay_seconds INT UNSIGNED DEFAULT NULL COMMENT '仅固定间隔模式有值',
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC' COMMENT '调度时区',
    enabled TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0停用 1启用；新建任务默认为停用',
    batch_size INT UNSIGNED NOT NULL DEFAULT 100 COMMENT '建议单批扫描数量',
    next_fire_at DATETIME(3) DEFAULT NULL COMMENT '下次计划触发时间（由调度器计算）',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_reliability_job_code (job_code),
    KEY idx_reliability_job_enabled_fire (enabled, next_fire_at, id),
    KEY idx_reliability_job_target (target_service, job_category),
    CONSTRAINT ck_reliability_job_category CHECK
        (job_category IN ('ORDER_TIMEOUT_SCAN', 'RETRY_SCAN', 'RECONCILIATION', 'CUSTOM')),
    CONSTRAINT ck_reliability_job_schedule CHECK
        ((schedule_mode = 1 AND cron_expression IS NOT NULL AND fixed_delay_seconds IS NULL)
         OR (schedule_mode = 2 AND cron_expression IS NULL AND fixed_delay_seconds IS NOT NULL AND fixed_delay_seconds > 0)),
    CONSTRAINT ck_reliability_job_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT ck_reliability_job_batch CHECK (batch_size > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='可靠性调度任务定义；任务调度与业务数据状态分离';
