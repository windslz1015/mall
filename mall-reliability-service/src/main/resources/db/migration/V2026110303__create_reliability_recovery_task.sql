-- 此表代表对业务域的一次“请求补偿/重试”工作，不是业务域最终状态的唯一真相。
-- business_key 必须指向一次稳定的业务操作（例如 order:123:release:456），而不是一个宽泛的订单ID。
-- 重试不会直接改订单、库存、支付数据库，必须经对应域的幂等接口执行。
CREATE TABLE reliability_recovery_task (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '补偿任务主键',
    task_no VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '任务唯一编号',
    task_type VARCHAR(64) NOT NULL COMMENT 'MESSAGE_RETRY / INVENTORY_RELEASE / ORDER_STATE_SYNC / OTHER等',
    business_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '同一补偿意图稳定幂等键',
    source_service VARCHAR(64) NOT NULL COMMENT '异常来源服务',
    source_event_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT '源事件ID（如有）',
    business_type VARCHAR(48) NOT NULL COMMENT '外部业务对象类型，如 ORDER / RESERVATION',
    business_id VARCHAR(128) NOT NULL COMMENT '外部业务对象ID（逻辑引用）',
    target_service VARCHAR(64) NOT NULL COMMENT '补偿动作真正所属业务服务',
    target_action VARCHAR(64) NOT NULL COMMENT '幂等业务操作编码（不存任意URL）',
    priority TINYINT UNSIGNED NOT NULL DEFAULT 2 COMMENT '1低 2普通 3高 4紧急',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0待处理 10执行中 20等待重试 30成功 40次数耗尽 50取消',
    max_attempts INT UNSIGNED NOT NULL DEFAULT 5 COMMENT '允许的最大执行次数',
    attempt_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已经尝试执行次数',
    next_execute_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '下一次允许执行时间',
    worker_id VARCHAR(128) DEFAULT NULL COMMENT '持有任务的执行实例',
    lease_token VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT '当前执行租约token',
    lease_expires_at DATETIME(3) DEFAULT NULL COMMENT '租约过期时间',
    context_json JSON DEFAULT NULL COMMENT '最小化、脱敏的业务定位上下文；禁存支付敏感信息',
    last_error_code VARCHAR(64) DEFAULT NULL COMMENT '最近错误码',
    last_error_summary VARCHAR(500) DEFAULT NULL COMMENT '脱敏错误摘要',
    finished_at DATETIME(3) DEFAULT NULL COMMENT '进入终态时间',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_reliability_task_no (task_no),
    UNIQUE KEY uk_reliability_task_dedup (task_type, business_key),
    KEY idx_reliability_task_due (status, next_execute_at, priority, id),
    KEY idx_reliability_task_lease (status, lease_expires_at),
    KEY idx_reliability_task_business (business_type, business_id),
    KEY idx_reliability_task_service (source_service, target_service, created_at),
    KEY idx_reliability_task_source_event (source_service, source_event_id),
    CONSTRAINT ck_reliability_task_priority CHECK (priority IN (1, 2, 3, 4)),
    CONSTRAINT ck_reliability_task_status CHECK (status IN (0, 10, 20, 30, 40, 50)),
    CONSTRAINT ck_reliability_task_attempts CHECK
        (max_attempts > 0 AND attempt_count <= max_attempts),
    CONSTRAINT ck_reliability_task_finished CHECK
        (status NOT IN (30, 40, 50) OR finished_at IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='跨域补偿与重试调度记录，业务状态仍由原业务域负责';
