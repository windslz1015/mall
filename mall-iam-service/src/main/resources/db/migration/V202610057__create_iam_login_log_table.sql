CREATE TABLE iam_login_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    account_id BIGINT UNSIGNED DEFAULT NULL COMMENT '账号ID',
    login_identifier VARCHAR(128) DEFAULT NULL COMMENT '登录标识',
    login_type TINYINT UNSIGNED NOT NULL
        COMMENT '登录方式：1用户名 2手机号 3邮箱',
    login_result TINYINT UNSIGNED NOT NULL
        COMMENT '结果：1成功 2失败',
    failure_reason VARCHAR(128) DEFAULT NULL COMMENT '失败原因',
    login_ip VARCHAR(45) DEFAULT NULL COMMENT '登录IP',
    user_agent VARCHAR(512) DEFAULT NULL COMMENT '客户端信息',
    login_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_account_login_time (
        account_id,
        login_at),
    KEY idx_result_login_time (
        login_result,
        login_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号登录日志';