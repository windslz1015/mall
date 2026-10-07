CREATE TABLE sys_account_role (
    id          BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    account_id  BIGINT UNSIGNED NOT NULL COMMENT '账号ID',
    role_id     BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    UNIQUE KEY uk_account_role (account_id, role_id),
    KEY idx_role_account (role_id, account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号角色关联表';