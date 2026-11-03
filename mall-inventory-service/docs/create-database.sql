-- 由拥有 CREATE DATABASE 权限的 MySQL 管理账号执行，仅创建数据库。
-- Flyway 迁移不负责 CREATE DATABASE，而是在运行时自动创建业务表和 history。
CREATE DATABASE IF NOT EXISTS mall_inventory
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 为 mall_inventory 配置独立的数据库账号；不要使用 root 运行应用。
-- 迁移账号需具备 CREATE、ALTER、INDEX、REFERENCES 和相关 DML 权限，
-- 运行账号按实际业务最小化 SELECT/INSERT/UPDATE/DELETE 权限。
-- 不在 Git 或配置示例里提交真实口令。
