# mall-reliability-service（DDD + Flyway 初始化版）

> 本压缩包**仅用于项目初始化**。没有自动扫描订单、没有 RabbitMQ 消费代码、没有业务补偿、没有对账或状态恢复实现。
mall-reliability-service 应该承担跨服务的异常恢复和补偿任务管理。
## 约定与目录

- 父工程 `com.mall:mall:1.0.0-SNAPSHOT`，Java 17、Spring Boot 3.5.13、Spring Cloud 2025.0.3、Spring Cloud Alibaba 2025.0.0.0。
- Maven 模块 `mall-reliability-service`，Java 包 `com.mall.reliability`，Nacos 服务名 `reliability-service`，默认端口 `8109`。
- 数据库 `mall_reliability`，MySQL 8.4、Flyway、MyBatis-Plus 3.5.15（对齐你现有项目习惯）。
- DDD 四层 `interfaces / application / domain / infrastructure`：应用编排、领域规则、持久化适配各自独立。
- 无业务实现的目录用 `package-info.java` 占位；只保留必要的领域状态枚举，不预制空 Service / Controller / Mapper。
- 此次不额外引入 RabbitMQ、Seata、XXL-JOB、OpenFeign 等依赖。真正开发对应功能时再引入。

```text
mall-reliability-service/
├── pom.xml
├── README.md
├── .env.example
├── src/main/java/com/mall/reliability/
│   ├── ReliabilityServiceApplication.java
│   ├── interfaces/{controller,handler}/
│   ├── application/{schedule,recovery,messaging,reconciliation}/
│   ├── domain/{schedule,recovery,messaging,reconciliation}/
│   └── infrastructure/
│       ├── config/
│       ├── client/{order,payment,inventory}/
│       ├── messaging/
│       ├── scheduling/
│       └── persistence/
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/V2026110301__... 至 V2026110308__...
└── docs/{create-database.sql,database-design.md,verify-schema.sql}
```

## Flyway 8 张表

| 表名 | 用途 |
| --- | --- |
| `reliability_job_definition` | 定时任务定义及触发策略 |
| `reliability_job_execution` | 扫描/调度每次执行记录 |
| `reliability_recovery_task` | 补偿和重试任务总记录 |
| `reliability_recovery_attempt` | 单次补偿执行尝试记录 |
| `reliability_message_failure` | 失败业务消息跟踪 |
| `reliability_reconciliation_batch` | 跨域对账批次 |
| `reliability_reconciliation_difference` | 对账差异及关联修复任务 |
| `reliability_reconciliation_resolution_log` | 对账差异处置/复核日志 |

完整字段、索引、主键、外键、唯一键、状态字典及边界原则参见 `docs/database-design.md`。
**跨服务订单/库存/支付记录是逻辑引用，不建跨数据库外键。**

## 安装与验证

1. 如存在之前生成的旧版非 DDD `mall-reliability-service/`，先备份并移走，不要混合覆盖。
2. 将整个目录放在 Maven 父工程根目录，与 `mall-iam-service/` 同级。
3. 在父 `pom.xml` 的 `<modules>` 中确保存在（不要重复）：
   `<module>mall-reliability-service</module>`。
4. 使用有创建数据库权限的账号执行 `docs/create-database.sql`，获得**空** `mall_reliability` 库。
5. 在 IDE/环境中设置 `.env.example` 中的 `RELIABILITY_DB_URL`、`RELIABILITY_DB_USERNAME`、`RELIABILITY_DB_PASSWORD`、`NACOS_SERVER_ADDR`、`NACOS_USERNAME`、`NACOS_PASSWORD`。`.env.example` 文件不会被 Spring 自动读取。
6. 在项目根目录执行：`./mvnw -pl mall-reliability-service -am clean package -DskipTests`，Windows 用 `.\mvnw.cmd`。
7. 启动 `ReliabilityServiceApplication`；Flyway 应在空库执行 8 条迁移。
8. 运行 `docs/verify-schema.sql` 核对 8 张表、迁移历史和索引。健康接口预期为 `http://localhost:8109/actuator/health`。

## 业务边界特别提醒

- 本服务将来**调度/请求** order-service 超时订单处理，而不直接写 order 数据库。
- 库存释放失败的实际补偿动作仍由 inventory-service 自己判断和执行。
- 支付是否扣款成功以 payment-service 的支付事实为准，本服务只辅助排查/对账。
- 消息失败记录不取代每个业务域的 Outbox/Inbox，也不意味着消息可以无条件重复投递。
- 平台 admin-service 的异常排查工单不等于本服务的补偿任务。

## 验证范围

打包前进行 XML/YAML/Java 17 语言级检查及 SQL 引用顺序、枚举状态对齐检查，但由于没有本机 MySQL 和 Maven 依赖仓库，**未实际执行 Flyway，也未完成 Spring Boot 全量构建和微服务联调**。
