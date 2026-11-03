-- 在部署 MySQL 8.x 的服务器上，使用拥有 CREATE DATABASE 权限的管理账号执行。
-- Flyway 迁移的前提是数据库存在；它负责建表，不负责创建数据库本身。
CREATE DATABASE IF NOT EXISTS mall_product
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 请使用专用账号连接本库，不共享 IAM、商家服务的数据库账号。
-- 账号由数据库管理员创建，授权请按运行时和迁移时职责区分：
-- Flyway迁移账号需要 CREATE、ALTER、INDEX、REFERENCES（以及必要的 DML 权限）。
-- 业务运行账号只需要 SELECT、INSERT、UPDATE、DELETE 权限。
-- 此处不附带明文密码，不在版本库记录真实账号密钥。
