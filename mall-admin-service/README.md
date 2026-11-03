# mall-admin-service（DDD + Flyway 初始化版）

> 本包**只做项目初始化**，没有用户监管、商家审核、商品强制下架、订单监管等业务实现。旧版非 DDD 的 `mall-admin-service` 不得直接与本包混合。

## 项目约定

- 继承现有 Maven 父工程 `com.mall:mall:1.0.0-SNAPSHOT`，Java 17、Spring Boot 3.5.13、Spring Cloud 2025.0.3、Spring Cloud Alibaba 2025.0.0.0。
- 模块名 `mall-admin-service`，基础包 `com.mall.admin`，Nacos 注册名 `admin-service`，默认端口 `8108`。
- 数据库 `mall_admin`，Flyway + MySQL + MyBatis-Plus 3.5.15（与 IAM 初始化惯例保持一致）。
- DDD 四层 `interfaces / application / domain / infrastructure`，未实现的业务包使用 `package-info.java` 占位，**没有空壳 Controller/Service/Mapper**。
- 本期不引入 OpenFeign、RabbitMQ、Seata：没有对应业务代码，直接依赖不是必要条件。

## 数据库：7 张表

| 表 | 用途 |
| --- | --- |
| `admin_operation_log` | 管理员操作审计 |
| `admin_regulation_case` | 监管案件主记录 |
| `admin_case_target` | 案件关联的业务对象 |
| `admin_case_action` | 监管处置请求与执行结果 |
| `admin_case_status_log` | 案件状态历史 |
| `admin_exception_investigation` | 异常排查工单 |
| `admin_investigation_log` | 异常排查过程历史 |

字段详情、外键、索引、CHECK、领域边界见 `docs/database-design.md`。`flyway_schema_history` 是 Flyway 自建表，不算业务表。

## DDD 目录

```text
com.mall.admin/
├── AdminServiceApplication.java
├── interfaces/
│   ├── controller/dto/
│   └── handler/
├── application/
│   ├── audit/{query,result,service}/
│   ├── regulation/{command,query,result,port,service}/
│   └── investigation/{command,query,result,port,service}/
├── domain/
│   ├── audit/{model,repository,service}/
│   ├── casefile/{model,repository,service}/
│   └── investigation/{model,repository,service}/
└── infrastructure/
    ├── config/
    ├── client/{iam,merchant,product,order,payment,inventory,reliability}/
    └── persistence/{audit,casefile,casetarget,caseaction,casestatus,investigation,investigationlog}/{po,mapper,repository}/
```

- `interfaces`：未来管理后台 API、请求参数校验、异常映射；不能写 SQL。
- `application`：未来监管、审计与异常排查用例编排，依赖领域仓储或应用端口。
- `domain`：未来监管案件聚合、目标、处置与状态规则；目前只包含状态枚举。
- `infrastructure`：未来 PO、Mapper、仓储实现、对 IAM/merchant/product/order/payment 的 HTTP 适配。

## 复制到现有工程

1. 先备份并移走任何旧的非 DDD 版 `mall-admin-service/`，**不要和本版合并覆盖**。
2. 解压 ZIP；将整个 `mall-admin-service/` 放在项目根目录，和 `mall-iam-service/` 同级。
3. 在根 `pom.xml` 的 `<modules>` 中确保有 `<module>mall-admin-service</module>`，不要重复添加。
4. 使用 MySQL DBA 账号执行 `docs/create-database.sql`，创建**空数据库** `mall_admin`。
5. 按 `.env.example` 设置环境变量 `ADMIN_DB_URL`、`ADMIN_DB_USERNAME`、`ADMIN_DB_PASSWORD`、`NACOS_SERVER_ADDR`、`NACOS_USERNAME`、`NACOS_PASSWORD`。`.env.example` 不会自动加载。
6. 在父工程运行：`./mvnw -pl mall-admin-service -am clean package -DskipTests`；Windows 用 `.\mvnw.cmd`。
7. 启动 `AdminServiceApplication`，Flyway 会执行 7 个版本的迁移，随后可用 `docs/verify-schema.sql` 检查。
8. 服务成功启动后，`http://localhost:8108/actuator/health` 用于健康检查；**没有业务 API 是正常的**。

## 重要设计说明

- `admin-service` 不是数据大仓库；查询用户、商家、商品、订单、支付时应通过目标业务域 API，不能跨库直接访问和修改。
- 实际商家审核记录在 `merchant-service`，商品强制下架记录在 `product-service`；本库留监管案件与处置操作记录，不重复存真实状态。
- 异常排查与 `reliability-service` 未来的重试/补偿调度是不同对象，不能把排查完成等同于补偿成功。
- 本次只进行 SQL/POM/YAML/Java 17 基础结构检查，不保证外部 MySQL/Nacos/Maven 依赖均可用。Flyway 迁移只针对空库，已有 `flyway_schema_history` 的旧库不得直接覆盖旧版脚本。
