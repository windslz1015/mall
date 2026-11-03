-- 使用具备 CREATE DATABASE 权限的 MySQL 管理账号运行一次。
-- 这里只创建数据库；九张业务表由应用启动时 Flyway 独立执行。
CREATE DATABASE IF NOT EXISTS mall_order
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 建议准备订单服务专用业务账号 order_app，并按开发/迁移/运行环境分权。
-- 不要将 MySQL root 密码、Nacos 密码写进源码仓库。
-- 注意 IF NOT EXISTS 不会清空旧库，也不会修复已存在的 Flyway 历史版本。
