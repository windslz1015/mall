-- 对账差异只保存定位信息和脱敏状态摘要，不复制订单项、渠道支付回调原文。
-- 同一业务对象可在不同批次发现差异：本表按 batch_id + difference_key 去重。
CREATE TABLE reliability_reconciliation_difference (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '对账差异ID',
    difference_no VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '差异唯一业务编号',
    batch_id BIGINT UNSIGNED NOT NULL COMMENT '所属对账批次ID',
    difference_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '批次内稳定差异去重键',
    rule_code VARCHAR(64) NOT NULL COMMENT '对账规则编码，例如 PAYMENT_SUCCESS_ORDER_PENDING',
    business_domain VARCHAR(20) NOT NULL COMMENT '不一致业务对象的归属域',
    business_type VARCHAR(48) NOT NULL COMMENT '业务对象类型',
    business_id VARCHAR(128) NOT NULL COMMENT '业务对象ID（跨域逻辑引用）',
    expected_state VARCHAR(128) DEFAULT NULL COMMENT '脱敏的应有状态或摘要',
    observed_state VARCHAR(128) DEFAULT NULL COMMENT '脱敏的实际状态或摘要',
    severity TINYINT UNSIGNED NOT NULL DEFAULT 2 COMMENT '1低 2普通 3高 4紧急',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0待处理 10已安排补偿 20已解决 30忽略',
    recovery_task_id BIGINT UNSIGNED DEFAULT NULL COMMENT '关联恢复/补偿任务（可选）',
    detected_at DATETIME(3) NOT NULL COMMENT '业务差异检测时间',
    resolved_at DATETIME(3) DEFAULT NULL COMMENT '差异解决或忽略时间',
    resolution_summary VARCHAR(500) DEFAULT NULL COMMENT '脱敏处理结论',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '追踪ID',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_reliability_difference_no (difference_no),
    UNIQUE KEY uk_reliability_difference_batch_key (batch_id, difference_key),
    KEY idx_reliability_difference_status (status, severity, detected_at, id),
    KEY idx_reliability_difference_business (business_domain, business_type, business_id),
    KEY idx_reliability_difference_task (recovery_task_id),
    KEY idx_reliability_difference_rule (rule_code, detected_at),

    CONSTRAINT ck_reliability_difference_domain CHECK
        (business_domain IN ('IAM', 'MERCHANT', 'PRODUCT', 'CART', 'ORDER', 'INVENTORY', 'PAYMENT', 'ADMIN')),
    CONSTRAINT ck_reliability_difference_severity CHECK (severity IN (1, 2, 3, 4)),
    CONSTRAINT ck_reliability_difference_status CHECK (status IN (0, 10, 20, 30)),
    CONSTRAINT ck_reliability_difference_resolved CHECK
        (status NOT IN (20, 30) OR resolved_at IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='跨域业务状态对账差异；恢复任务由自身域执行，最终结果应复核';
