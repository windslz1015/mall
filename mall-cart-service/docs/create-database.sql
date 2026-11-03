-- 用有 CREATE DATABASE 权限的 MySQL 管理账号执行一次。
-- 这里只创建独立数据库，不直接建业务表；业务表由 Flyway 按版本执行。
CREATE DATABASE IF NOT EXISTS mall_cart
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 建议为 mall_cart 使用独立数据库账号，例如 cart_app。
-- 不在源码中存放真实数据库密码；迁移账号需 CREATE / ALTER / INDEX / REFERENCES / DML 权限。
-- 注意：CREATE DATABASE IF NOT EXISTS 不会清除已有数据。
