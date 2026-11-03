-- 由有 CREATE DATABASE 权限的 DBA 执行一次；不包含业务表的建表 SQL。
-- admin-service 启动时 Flyway 会创建 flyway_schema_history 并执行 7 个迁移脚本。
CREATE DATABASE IF NOT EXISTS mall_admin
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 建议分配 admin_app 专用账号的 mall_admin 库权限，不应使用 MySQL root 作为应用账号。
-- 生产中 Flyway 迁移账号建议与日常运行的 DML 账号分离。
-- 当前脚本没有任何硬编码密码，也不清除现有数据。
