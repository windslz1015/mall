-- 一次性的数据库准备 SQL：请以具备 CREATE DATABASE 权限的管理账号执行。
-- Flyway 不创建数据库，Flyway 在 mall_merchant 数据库已存在后运行表结构迁移。
CREATE DATABASE IF NOT EXISTS mall_merchant
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 建议为本业务库单独创建最小权限数据库用户；账号密码由部署环境管理。
-- 示例（请先创建用户，再按实际环境授权）：
-- GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
--     ON mall_merchant.* TO 'merchant_app'@'%';
