# mall-payment-service（DDD + Flyway 初始化版）

> **只初始化工程与支付库，不含业务接口和支付执行代码。** 旧非 DDD 版本 `mall-payment-service` 不要与本包混合覆盖。

## 环境与项目约定

- Maven 父工程 `com.mall:mall:1.0.0-SNAPSHOT`；Java 17、Spring Boot 3.5.13、Spring Cloud 2025.0.3、Alibaba 2025.0.0.0（版本由你原来的父工程管理）。
- 服务模块 `mall-payment-service`；包名 `com.mall.payment`；应用名 `payment-service`；默认端口 `8107`。
- 独立数据库 `mall_payment`；Flyway Core + Flyway MySQL；Spring MVC、MyBatis-Plus 3.5.15、Nacos Discovery（项目现有约定）。
- DDD `interfaces / application / domain / infrastructure` 四层，只用 `package-info.java` 预留未来包。
- 不引入支付 SDK、RabbitMQ 或 Seata 依赖：当前没有编写对应业务代码。Outbox / Inbox **仅建表**。

## 8 张 Flyway 表

| 表 | 含义 |
| --- | --- |
| `payment_order` | 订单对应支付单、金额、币种和状态 |
| `payment_attempt` | 每次向支付渠道发起的支付尝试 |
| `payment_record` | 已确认成功扣款的资金事实（保留异常重复扣款记录） |
| `payment_notification` | 回调验签及去重记录（不存原始敏感报文） |
| `payment_operation` | 支付关键操作幂等记录 |
| `payment_status_log` | 支付单状态变更记录 |
| `outbox_event` | 未来可靠事件生产端预留表 |
| `inbox_event` | 未来消息消费去重预留表 |

完整字段、索引、状态、约束、跨服务边界见 `docs/database-design.md`。

## DDD 目录

```text
com.mall.payment/
├── PaymentServiceApplication.java
├── interfaces/
│   ├── controller/dto/
│   └── handler/
├── application/
│   ├── payment/{command,query,result,port,service}/
│   ├── attempt/{command,service}/
│   ├── notification/{command,service}/
│   ├── reconciliation/{port,service}/
│   └── reliability/{port,service}/
├── domain/
│   ├── payment/{model,repository,service}/
│   ├── attempt/{model,repository,service}/
│   ├── notification/{model,repository,service}/
│   ├── record/{model,repository,service}/
│   └── operation/{model,repository,service}/
└── infrastructure/
    ├── config/
    ├── client/order/
    ├── channel/
    ├── messaging/{outbox,inbox}/
    └── persistence/{payment,attempt,notification,record,operation,status,outbox,inbox}/{po,mapper,repository}/
```

### 分层职责

- `interfaces`：未来支付 HTTP 请求与校验，只调用应用层，不写 SQL。
- `application`：未来支付创建、渠道发起、结果应用等业务用例编排，依赖 Domain Repository 与 Port。
- `domain`：未来支付单聚合、支付状态转换、支付幂等规则；目前只有 5 个状态枚举。
- `infrastructure`：未来数据库 PO / Mapper / Repository 实现、第三方支付 API 适配和消息适配。

## 导入说明

1. **如已导入旧版非 DDD 的 `mall-payment-service`，先备份旧目录并完整移走，不要和新目录合并。**
2. 解压 `mall-payment-service/` 到父工程根目录，与 `mall-iam-service/` 同级。
3. 根 `pom.xml` 的 `<modules>` 中加入 `<module>mall-payment-service</module>`，如果已存在则不要重复加。
4. 以 MySQL 管理账号执行 `docs/create-database.sql`，确保新建的是**空库**。
5. 参考 `.env.example` 将 `PAYMENT_DB_URL`、`PAYMENT_DB_USERNAME`、`PAYMENT_DB_PASSWORD`、`NACOS_SERVER_ADDR`、`NACOS_USERNAME`、`NACOS_PASSWORD` 配置到本机环境或 IDE（不会自动读取 `.env.example`）。
6. 在根工程运行 `./mvnw -pl mall-payment-service -am clean package -DskipTests`；Windows 可用 `.\mvnw.cmd -pl mall-payment-service -am clean package -DskipTests`。
7. 启动 `PaymentServiceApplication` 后 Flyway 自动执行 8 个迁移脚本；执行 `docs/verify-schema.sql` 检查表、索引和迁移记录。
8. 数据库、Nacos 均可连接时访问 `http://localhost:8107/actuator/health` 检查服务健康状态。

## 重要说明

- 数据库层的回调记录、幂等和 Outbox 表**不代表**已经有回调验签、幂等或消息重试能力。
- 本期一个交易主订单对应一笔支付单；失败重试对应新的 `payment_attempt`。超时、并发和订单状态同步由后续业务实现。
- **未做退款**，没有退款流水与商家分账；如果未来引入退款/结算，需要新设计业务对象并追加迁移脚本。
- Flyway 已执行版本不可直接改写；当前脚本只适用于新空库，和旧版支付服务表结构不保证兼容。
- 本包在交付前进行基础语法、XML / YAML、SQL 文件依赖顺序和 ZIP 完整性检查。由于无真实数据库与 Maven 外部依赖环境，**没有证明可以直接完成全量 Maven 构建及真实 Flyway 迁移**。
