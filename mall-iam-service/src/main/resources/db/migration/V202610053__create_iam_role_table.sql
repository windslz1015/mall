CREATE TABLE iam_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    account_type TINYINT UNSIGNED DEFAULT NULL
        COMMENT '适用账号类型',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1
        COMMENT '状态：1启用 2禁用',
    built_in TINYINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '是否系统内置：0否 1是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code),
    KEY idx_account_type_status (
        account_type,
        status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';