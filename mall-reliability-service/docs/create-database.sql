-- DBA 使用具备 CREATE DATABASE 权限的账号执行一次。
-- 本脚本只创建空库，不在这里执行 CREATE TABLE；Flyway 首次启动时负责迁移。
CREATE DATABASE IF NOT EXISTS mall_reliability
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 建议为 reliability-service 创建专用数据库账号，限定在 mall_reliability。
-- 示例：RELIABILITY_DB_USERNAME=reliability_app；密码由部署环境注入而非硬编码。
-- 生产环境可将 Flyway DDL 迁移账号与应用日常 DML 账号分开。
