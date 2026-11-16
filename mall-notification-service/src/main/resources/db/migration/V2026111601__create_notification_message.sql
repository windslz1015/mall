-- 用户站内通知独立归属 notification-service；account_id 是 IAM ID 的跨服务逻辑引用，不建跨库外键。
CREATE TABLE notification_message (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '站内通知ID',
    account_id BIGINT UNSIGNED NOT NULL COMMENT 'IAM账号ID（跨服务逻辑引用）',
    biz_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '业务幂等键',
    source_event_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '来源事件ID',
    type VARCHAR(32) NOT NULL COMMENT 'WELCOME，预留订单等其他类型',
    title VARCHAR(100) NOT NULL COMMENT '通知标题',
    content VARCHAR(500) NOT NULL COMMENT '站内通知正文；不存密码、验证码等敏感信息',
    read_at DATETIME(3) DEFAULT NULL COMMENT '用户阅读时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_biz_key (biz_key),
    KEY idx_notification_account_created (account_id, created_at, id),
    CONSTRAINT ck_notification_account_id CHECK (account_id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='用户站内通知';
