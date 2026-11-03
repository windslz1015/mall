-- 平台异常排查“工单”：只记录排查过程；补偿任务与重试次数归 reliability-service。
-- detection_key 对单次异常事件稳定，用于避免重复上报同一事件。
CREATE TABLE admin_exception_investigation (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '异常排查工单主键',
    investigation_no VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '排查单编号',
    detection_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT '单次异常事件稳定去重键；不同异常批次应生成不同键',
    anomaly_type VARCHAR(64) NOT NULL COMMENT '异常类型，如 ORDER_PAYMENT_MISMATCH',
    source_domain VARCHAR(20) NOT NULL COMMENT '异常归属域',
    source_type VARCHAR(48) NOT NULL COMMENT '异常资源类型',
    source_id VARCHAR(64) NOT NULL COMMENT '外部异常资源ID（跨域逻辑引用）',
    priority TINYINT UNSIGNED NOT NULL DEFAULT 2 COMMENT '1低 2普通 3高 4紧急',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0待排查 10排查中 20已解决 30关闭',
    details VARCHAR(2000) DEFAULT NULL COMMENT '已脱敏的异常情况摘要',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '原始业务请求链路ID',
    assigned_to_account_id BIGINT UNSIGNED DEFAULT NULL COMMENT 'IAM被指派的管理员账号ID',
    related_case_id BIGINT UNSIGNED DEFAULT NULL COMMENT '若升级为监管案件则关联案件ID',
    detected_at DATETIME(3) NOT NULL COMMENT '异常首次发现时间',
    resolved_at DATETIME(3) DEFAULT NULL COMMENT '异常解决时间',
    resolution_summary VARCHAR(1000) DEFAULT NULL COMMENT '脱敏排查结论',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '后续并发更新乐观锁版本',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_investigation_no (investigation_no),
    UNIQUE KEY uk_admin_investigation_detection_key (detection_key),
    KEY idx_admin_investigation_status_priority (status, priority, detected_at, id),
    KEY idx_admin_investigation_assignee (assigned_to_account_id, status, detected_at),
    KEY idx_admin_investigation_resource (source_domain, source_type, source_id),
    KEY idx_admin_investigation_case (related_case_id),
    CONSTRAINT fk_admin_investigation_case FOREIGN KEY (related_case_id)
        REFERENCES admin_regulation_case (id),
    CONSTRAINT ck_admin_investigation_source CHECK
        (source_domain IN ('IAM', 'MERCHANT', 'PRODUCT', 'ORDER', 'INVENTORY', 'PAYMENT', 'RELIABILITY')),
    CONSTRAINT ck_admin_investigation_priority CHECK (priority IN (1, 2, 3, 4)),
    CONSTRAINT ck_admin_investigation_status CHECK (status IN (0, 10, 20, 30)),
    CONSTRAINT ck_admin_investigation_resolved CHECK
        (status NOT IN (20, 30) OR resolved_at IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='平台异常查询与人工排查工单；不承载补偿任务执行';
