-- 由有建库权限的管理账号执行一次。Flyway会自动建表，但不会自动创建数据库。
CREATE DATABASE IF NOT EXISTS mall_notification
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 建议单独创建 notification_app 用户，并仅授权 mall_notification。
-- 迁移账号需必要DDL权限，业务运行账号只需最低限度DML权限。
-- 本文件不包含实际数据库账号密码。
