CREATE TABLE iam_account (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '账号ID',
    account_no VARCHAR(32) NOT NULL COMMENT '账号编号',
    account_type TINYINT UNSIGNED NOT NULL
        COMMENT '账号类型：1普通用户 2商家 3平台管理员',
    username VARCHAR(64) DEFAULT NULL COMMENT '登录用户名',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1
        COMMENT '状态：1正常 2禁用 3锁定 4注销',
    failed_login_count INT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '连续登录失败次数',
    locked_until DATETIME(3) DEFAULT NULL COMMENT '锁定截止时间',
    password_updated_at DATETIME(3) DEFAULT NULL COMMENT '密码更新时间',
    last_login_at DATETIME(3) DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(45) DEFAULT NULL COMMENT '最后登录IP',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_no (account_no),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone),
    UNIQUE KEY uk_email (email),
    KEY idx_type_status_created (
        account_type,
        status,
        created_at,
        id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一账号表';