CREATE TABLE iam_permission (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    permission_code VARCHAR(128) NOT NULL COMMENT '权限编码',
    permission_name VARCHAR(128) NOT NULL COMMENT '权限名称',
    domain_code VARCHAR(64) NOT NULL COMMENT '所属业务域',
    resource_code VARCHAR(64) NOT NULL COMMENT '资源',
    action_code VARCHAR(64) NOT NULL COMMENT '操作',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1
        COMMENT '状态：1启用 2禁用',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (permission_code),
    KEY idx_domain_status (
        domain_code,
        status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';