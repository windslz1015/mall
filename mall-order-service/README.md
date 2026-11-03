# mall-order-service（DDD + Flyway 初始化版）

> **仅初始化工程与数据库**。不含订单提交、拆单、取消、支付回调、定时任务、库存远程调用等业务代码。旧的非DDD订单服务压缩包请勿与本包混合覆盖。

## 1. 兼容现有项目

- 继承根 POM：`com.mall:mall:1.0.0-SNAPSHOT`，Java 17 / Spring Boot 3.5.13 / Spring Cloud 2025.0.3 / Alibaba 2025.0.0.0。
- 依赖 `mall-common`，包名 `com.mall.order`；坚持 `interfaces / application / domain / infrastructure` 四层。
- Nacos Discovery + Spring MVC + MyBatis-Plus 3.5.15 + MySQL 8.4 + Flyway Core/MySQL。
- `spring.application.name=order-service`，默认端口 `8105`，独立数据库 `mall_order`。
- 业务尚未实现的包使用 `package-info.java` 说明职责，没有假接口/空 Service/SQL in Application。

## 2. Flyway 九张业务表

| 表名 | 负责内容 |
| --- | --- |
| `trade_order` | 主订单、应付金额、支付截止时间、主状态 |
| `trade_sub_order` | 按商家拆单、商家主体快照、子订单状态 |
| `trade_order_item` | SPU/SKU、名称、规格、成交价格等订单项快照 |
| `trade_order_operation` | 提交、取消、超时、应用支付结果等操作幂等 |
| `trade_order_cancel_record` | 用户/超时/系统最终取消结果 |
| `trade_order_status_log` | 主订单状态变更历史 |
| `trade_sub_order_status_log` | 子订单状态变更历史 |
| `outbox_event` | 订单侧待投递业务消息（只建表） |
| `inbox_event` | 订单侧消费去重（只建表） |

`flyway_schema_history` 是 Flyway 自身自动生成的迁移版本表，不计为业务表。
详细业务边界、状态、金额、索引与 FK 设计请看 `docs/database-design.md`。

## 3. DDD 目录

```text
com.mall.order/
├── OrderServiceApplication.java
├── interfaces/
│   ├── controller/dto/
│   └── handler/
├── application/
│   ├── order/{command,query,result,port,service}/
│   ├── cancellation/{command,service}/
│   ├── payment/{command,service}/
│   ├── timeout/service/
│   └── reliability/{port,service}/
├── domain/
│   ├── order/{model,repository,service}/
│   ├── suborder/{model,repository,service}/
│   ├── item/{model,repository}/
│   ├── cancellation/{model,repository,service}/
│   └── operation/{model,repository,service}/
└── infrastructure/
    ├── config/
    ├── client/{product,merchant,inventory,payment}/
    ├── messaging/{outbox,inbox}/
    └── persistence/{order,suborder,item,operation,cancellation,status,outbox,inbox}/{po,mapper,repository}/
```

- `domain` 将来定义聚合根、订单业务规则与 Repository 抽象。
- `application` 将来执行下单编排，协调商品/商家/库存等 Port；不直接操作数据库 Mapper/JdbcTemplate。
- `infrastructure` 将来实现 Repository（PO + Mapper）、外部 HTTP 适配器和可靠消息。
- `interfaces` 将来只负责协议参数、身份入口和响应，不直接写业务 SQL。

## 4. 导入步骤

1. **如果项目里还有早期生成的 `mall-order-service`，先备份或删除旧的整个目录**，不要把两套 Java 类混在同一包里。
2. 解压整个 `mall-order-service/` 文件夹到根项目目录，和 `mall-iam-service/` 同级。
3. 在根 `pom.xml` 的 `<modules>` 中确认有 `<module>mall-order-service</module>`，只保留一次。
4. 以 MySQL 管理账号执行 `docs/create-database.sql` 创建**空的** `mall_order` 数据库。
5. 设置 `ORDER_DB_URL` / `ORDER_DB_USERNAME` / `ORDER_DB_PASSWORD` / `NACOS_SERVER_ADDR` / `NACOS_USERNAME` / `NACOS_PASSWORD` 环境变量，参照 `.env.example`（Spring 不会自动加载该文件）。
6. 执行编译后启动 `OrderServiceApplication`，Flyway 自动运行九个迁移文件。
7. 执行 `docs/verify-schema.sql` 验证表、迁移历史、FK 和 CHECK。

Linux/macOS 根目录构建：

```bash
./mvnw -pl mall-order-service -am clean package -DskipTests
```

Windows PowerShell：

```powershell
.\mvnw.cmd -pl mall-order-service -am clean package -DskipTests
```

服务启动且数据库、Nacos 可用后，查看 `http://localhost:8105/actuator/health`。

## 5. 注意

- **必须先有空库**。Flyway 脚本命名 `V2026110301` 起，仅在本订单数据库内部排序；其他服务使用相同版本号不冲突，因为库隔离。
- 新的 `trade_order` 表与早期生成的非DDD订单数据库不保证兼容，请勿在已有业务数据的库中直接运行此包的建表脚本。
- 当前没有设定支付超时时长，`pay_deadline_at` 由后续业务实现根据产品规则计算。表上的索引不意味着已经有自动取消任务。
- `outbox_event`、`inbox_event` 只是待实现的可靠消息存储结构，未添加 RabbitMQ 执行代码。
- 真实 Maven 依赖下载、MySQL 8.4 Flyway 执行、Nacos 注册请在实际环境验证；目前仅进行了静态结构检查与 Java 17 枚举编译。
