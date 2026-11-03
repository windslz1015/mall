-- 支付发起、处理回调、关闭支付、异常补偿等操作的通用幂等记录。
-- 同一调用方 + 操作类型 + request_key 唯一，并用 request_fingerprint 对比载荷。
CREATE TABLE payment_operation (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付关键操作主键',
    payment_order_id BIGINT UNSIGNED DEFAULT NULL COMMENT '关联支付单（首次创建前可为空）',
    source_service VARCHAR(64) NOT NULL COMMENT '请求源系统或渠道标识',
    operation_type VARCHAR(32) NOT NULL COMMENT 'CREATE/INITIATE/APPLY_RESULT/CLOSE/RECONCILE 等',
    request_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '调用方提供的稳定幂等键',
    request_fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT '请求规范化内容SHA-256，重复键必须与原值相同',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1处理中 2成功 3失败',
    result_code VARCHAR(64) DEFAULT NULL COMMENT '业务处理结果代码',
    error_summary VARCHAR(500) DEFAULT NULL COMMENT '脱敏错误摘要',
    completed_at DATETIME(3) DEFAULT NULL COMMENT '完成时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_operation_request (source_service, operation_type, request_key),
    KEY idx_payment_operation_order (payment_order_id, created_at, id),
    KEY idx_payment_operation_status_updated (status, updated_at, id),
    CONSTRAINT fk_payment_operation_order FOREIGN KEY (payment_order_id)
        REFERENCES payment_order (id),
    CONSTRAINT ck_payment_operation_status CHECK (status IN (1, 2, 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='支付关键操作幂等记录（只建表，不实现幂等逻辑）';
