# mall-inventory-service（DDD + Flyway 初始化版）

> **只初始化服务工程和数据模型**；没有库存查询、调整、锁定、确认、释放等业务功能，不要把本包当成已完成的库存微服务。

## 1. 兼容你的现有工程

- 父项目 `com.mall:mall:1.0.0-SNAPSHOT`，Java 17，Spring Boot 3.5.13；Spring Cloud / Alibaba 版本由父 POM 管理。
- 复用 `mall-common`，采用与 IAM、商品服务相同的 `interfaces / application / domain / infrastructure` 分层。
- Nacos Discovery、Spring Boot MVC、MyBatis-Plus 3.5.15、MySQL 8.4、Flyway core + MySQL 方言。
- `spring.application.name=inventory-service`，默认启动端口 `8106`，独立数据库 `mall_inventory`。
- 业务包以 `package-info.java` 占位，没有空 Controller、Service 或虚假的 CRUD 功能。

## 2. 业务表（6 张）

| 表名 | 用途 |
| --- | --- |
| `inventory_stock` | SKU 实有、可用与锁定库存余额 |
| `inventory_reservation` | 订单明细级预占、确认和释放状态 |
| `inventory_operation` | 每次成功库存变更的幂等操作记录 |
| `inventory_adjustment` | 补货、盘点、系统调整原因与操作审计 |
| `inventory_stock_ledger` | 库存变化前后数量、变化量、历史流水 |
| `inventory_reservation_event` | 预占状态变化历史 |

MySQL 会再由 Flyway 创建 `flyway_schema_history`，不计入业务表。

库存数量口径必须遵守：**实有库存 = 可用库存 + 已锁定库存**。详细字段、索引、FK、状态机、幂等及一致性约束见 `docs/database-design.md`。

## 3. DDD 包布局

```text
com.mall.inventory
├── interfaces/                 # Controller / DTO / Handler 预留
├── application/                # stock/reservation/adjustment 用例与Port预留
├── domain/                     # stock/reservation/operation/adjustment/ledger
│   ├── stock/model/            # 库存余额领域模型预留
│   ├── reservation/model/      # StockReservationStatus 枚举
│   ├── operation/model/        # InventoryOperationType 枚举
│   └── adjustment/model/       # InventoryOperatorType 枚举
└── infrastructure/             # PO / Mapper / Repository实现 / 外部客户端预留
    ├── client/product/         # future SKU query
    ├── client/merchant/        # future merchant authorization
    ├── client/order/           # future order validation
    └── persistence/            # stock, reservation, operation, adjustment, ledger
```

## 4. 安装步骤

1. 解压后把**整个 `mall-inventory-service/` 文件夹**放到项目根目录，与 `mall-product-service` 同级，不覆盖现有 IAM/Gateway/商品/商家服务。
2. 父 `pom.xml` 的 `<modules>` 中保留且只保留一次 `<module>mall-inventory-service</module>`。
3. 在 MySQL 执行 `docs/create-database.sql`，准备本库独立用户及授权，切勿使用 root 作为服务运行账号。
4. 在 IDE 或 shell 设置以下环境变量：`INVENTORY_DB_URL`、`INVENTORY_DB_USERNAME`、`INVENTORY_DB_PASSWORD`、`NACOS_SERVER_ADDR`、`NACOS_USERNAME`、`NACOS_PASSWORD`。参见 `.env.example`。
5. 运行 `InventoryServiceApplication`；启动时 Flyway 自动执行 6 个脚本并生成 `flyway_schema_history`。
6. 执行 `docs/verify-schema.sql` 核实表、外键和迁移记录。

构建（根 POM 声明的其他模块必须确实存在）：

```bash
./mvnw -pl mall-inventory-service -am clean package -DskipTests
```

Windows PowerShell：

```powershell
.\mvnw.cmd -pl mall-inventory-service -am clean package -DskipTests
```

当服务已启动、Nacos/MySQL 正常时，可访问：`http://localhost:8106/actuator/health`。

## 5. 约束与注意事项

- `sku_id`、`product_id` 与商家主体 `merchant_id` 不设置跨服务外键；库存只存 SKU 维度数量，不存售价/商品信息。
- **锁定库存不会自动超时释放**；本包只有数据库中用于记录失效时间的字段，还没有任务或 API。
- Flyway 负责建表，不意味着已经实现并发防超卖、请求幂等和订单支付一致性；这些需要未来的本地事务、状态机、条件更新及跨服务补偿。
- 初始化脚本**仅用于空数据库**。旧库存服务的历史迁移不能随意覆盖或重新命名。
- 真实 Maven 依赖拉取、MySQL 8.4 Flyway 执行、Nacos 注册需在你的环境中验证。
