# mall-merchant-service — 商家服务（DDD 初始化版）

**本包重新从零生成，不采用上一批“业务逻辑 + JDBC 混层”的实现。**

## 交付范围

- 与现有项目保持一致：Java 17 / Spring Boot 3.5.13（继承父 POM）/ `com.mall` / Nacos / MyBatis-Plus / MySQL 8.4 / Flyway。
- 规范包结构：`interfaces`、`application`、`domain`、`infrastructure`，和 IAM 的 DDD 分层一致。
- 仅实现服务启动类、状态码枚举及包职责占位；**不实现商家入驻、审核、增删改查等功能**。
- Flyway 共 **6 个 migration、6 张业务表**，覆盖商家入驻、审核、主体、资料、账号关联、启停与审计。
- 每张表的字段、索引、约束、状态码详见 `docs/database-design.md`。

## 目录关键位置

```text
mall-merchant-service/
├── pom.xml
├── README.md
├── .env.example
├── docs/
│   ├── create-database.sql
│   └── database-design.md
└── src/main/
    ├── java/com/mall/merchant/
    │   ├── MerchantServiceApplication.java
    │   ├── interfaces/controller/           # HTTP入口，待功能开发
    │   ├── application/merchant/            # 用例编排与Port，待功能开发
    │   ├── domain/application/              # 入驻申请领域模型与仓储契约
    │   ├── domain/merchant/                 # 商家领域模型与仓储契约
    │   └── infrastructure/                  # Mapper、PO、Repository 实现待补
    └── resources/
        ├── application.yml
        └── db/migration/                    # 六个 Flyway 版本脚本
```

为防止 Java 包目录在 Git 中丢失，未实现的子包采用 `package-info.java` 声明用途。未添加假的 Controller / Service / Mapper 来充数。后续真正开发时按目录逐层补充。

## 集成到你当前项目

1. 解压 `mall-merchant-service` 到 `mall-iam-service` 同级。**不要覆盖 IAM、Gateway 和 mall-common**。
2. 检查父工程 `pom.xml` 中有 `<module>mall-merchant-service</module>`。
3. 如果父工程已经提前声明其他尚未解压的模块，暂时注释不存在的 `<module>`，否则根工程 Maven 会因缺失目录报错。
4. 在 MySQL 执行 `docs/create-database.sql`；为业务服务单独准备 MySQL 账号并授予该库的迁移所需权限。
5. 配置 `MERCHANT_DB_URL`、`MERCHANT_DB_USERNAME`、`MERCHANT_DB_PASSWORD`、`NACOS_SERVER_ADDR`、`NACOS_USERNAME`、`NACOS_PASSWORD` 环境变量。
6. 在根目录运行：`./mvnw -pl mall-merchant-service -am clean package`，或 Windows 上运行 `mvnw.cmd -pl mall-merchant-service -am clean package`。
7. 运行启动类 `com.mall.merchant.MerchantServiceApplication`，Spring Boot 启动时自动触发 Flyway migration。

## 验证建表

```sql
USE mall_merchant;
SHOW TABLES;
SELECT installed_rank, version, description, success
FROM flyway_schema_history ORDER BY installed_rank;
```

应看到 6 张以 `merchant_` 开头的业务表和 1 张 `flyway_schema_history`。服务健康检查：`http://localhost:8102/actuator/health`（服务与依赖都已启动时）。

## 注意

- 仅建表，不实现业务接口；健康检查不等于业务可用。
- 没有商家账号创建逻辑；现有 IAM 还需单独支持商家账号/商家角色后才能实现真正入驻流程。
- 没有在商家库复制 IAM 的账号信息，也没有在跨服务账号 ID 上添加 FK。
- 本包使用独立数据库 `mall_merchant`，不要求修改现有 IAM Flyway 脚本。
- 当前交付已检查 XML、YAML、Java 语法和 ZIP，但尚未在真实 MySQL / Nacos 环境执行 Flyway 迁移或 Maven 联调。
