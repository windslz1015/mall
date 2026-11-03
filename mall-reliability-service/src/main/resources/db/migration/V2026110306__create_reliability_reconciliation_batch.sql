-- 一批对账检查的时间窗和执行状态；[window_start_at, window_end_at) 左闭右开。
-- run_round 用于相同业务时间窗下的人工重跑或新增复核批次。
CREATE TABLE reliability_reconciliation_batch (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '对账批次ID',
    batch_no VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '对账批次唯一号',
    reconciliation_type VARCHAR(48) NOT NULL COMMENT 'ORDER_PAYMENT / ORDER_INVENTORY / PAYMENT_CHANNEL / CUSTOM',
    window_start_at DATETIME(3) NOT NULL COMMENT '业务检查时间窗起点（含）',
    window_end_at DATETIME(3) NOT NULL COMMENT '业务检查时间窗终点（不含）',
    run_round INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '相同时间窗第几次核对',
    job_execution_id BIGINT UNSIGNED DEFAULT NULL COMMENT '由定时任务发起时关联本地执行记录',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0待运行 10核对中 20完成 30失败 40取消',
    cursor_token VARCHAR(255) DEFAULT NULL COMMENT '不含敏感信息的续扫标记',
    scanned_count BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已核验业务记录数',
    matched_count BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '核验一致的记录数',
    difference_count BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '本次发现差异数（一个对象可以命中多条规则）',
    started_at DATETIME(3) DEFAULT NULL COMMENT '执行起始时间',
    finished_at DATETIME(3) DEFAULT NULL COMMENT '执行结束时间',
    failure_summary VARCHAR(500) DEFAULT NULL COMMENT '脱敏失败说明',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_reliability_reconciliation_batch_no (batch_no),
    UNIQUE KEY uk_reliability_reconciliation_window (
        reconciliation_type, window_start_at, window_end_at, run_round),
    KEY idx_reliability_reconciliation_type_status (reconciliation_type, status, created_at, id),
    KEY idx_reliability_reconciliation_execution (job_execution_id),
    KEY idx_reliability_reconciliation_time (window_start_at, window_end_at),
    CONSTRAINT fk_reliability_reconciliation_execution FOREIGN KEY (job_execution_id)
        REFERENCES reliability_job_execution (id),
    CONSTRAINT ck_reliability_reconciliation_type CHECK
        (reconciliation_type IN ('ORDER_PAYMENT', 'ORDER_INVENTORY', 'PAYMENT_CHANNEL', 'CUSTOM')),
    CONSTRAINT ck_reliability_reconciliation_window CHECK (window_end_at > window_start_at),
    CONSTRAINT ck_reliability_reconciliation_round CHECK (run_round > 0),
    CONSTRAINT ck_reliability_reconciliation_status CHECK (status IN (0, 10, 20, 30, 40)),
    CONSTRAINT ck_reliability_reconciliation_counts CHECK (matched_count <= scanned_count),
    CONSTRAINT ck_reliability_reconciliation_finished CHECK
        (status NOT IN (20, 30, 40) OR finished_at IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='订单/支付/库存跨域一致性对账批次；以业务域查询结果核验';
