-- 订单侧关键操作幂等记录：提交、取消、超时、支付结果应用、补偿。
-- 相同 source_service + operation_type + request_key 仅允许一条；
-- 重复调用须核对 request_fingerprint（规范化入参的SHA-256）。
-- 此表不能代替订单状态机、数据库条件更新或异步消息对账。
CREATE TABLE trade_order_operation (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单关键业务操作ID',
    source_service VARCHAR(64) NOT NULL COMMENT '操作发起方，如 buyer-web/payment-service/order-scheduler',
    operation_type TINYINT UNSIGNED NOT NULL COMMENT '1提交 2用户取消 3超时取消 4确认支付成功 5处理支付失败 6补偿',
    request_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT '上游请求幂等键（区分大小写）',
    request_fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT '规范化业务参数SHA-256哈希',
    target_order_id BIGINT UNSIGNED DEFAULT NULL COMMENT '本次操作关联的交易主订单ID',
    actor_account_id BIGINT UNSIGNED DEFAULT NULL COMMENT '人工发起操作的IAM账号ID（逻辑引用）',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1处理中 2成功 3失败',
    error_code VARCHAR(64) DEFAULT NULL COMMENT '最后业务失败码',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '最后错误摘要，不包含敏感信息',
    retry_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前操作尝试次数',
    completed_at DATETIME(3) DEFAULT NULL COMMENT '最近完成时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_trade_order_operation_request
        (source_service, operation_type, request_key),
    KEY idx_trade_order_operation_order (target_order_id, operation_type, id),
    KEY idx_trade_order_operation_status_time (status, updated_at, id),
    CONSTRAINT ck_trade_order_operation_type CHECK (operation_type IN (1, 2, 3, 4, 5, 6)),
    CONSTRAINT ck_trade_order_operation_status CHECK (status IN (1, 2, 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='订单关键操作去重与执行结果（本地事务协调）';
