-- 由拥有 CREATE DATABASE 权限的管理员执行一次；仅建数据库。
-- 8 张业务表及 flyway_schema_history 由 PaymentServiceApplication 启动时迁移。
CREATE DATABASE IF NOT EXISTS mall_payment
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 建议建立专用的 payment_app 数据库账号，开发环境使用必要权限。
-- 生产环境可将 Flyway 迁移用户与日常应用 DML 用户分离。
-- 本脚本不包含任何默认或明文数据库密码。
-- IF NOT EXISTS 不会清空旧表，也不保证能兼容旧版 Flyway 迁移历史。
