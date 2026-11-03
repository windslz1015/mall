# mall-cart-service（DDD + Flyway 初始化版）

> **仅创建工程骨架与数据库结构**。本包不含购物车 CRUD、远程商品查询、库存锁定或订单协作业务实现。

## 1. 与已有项目保持一致

- 父 POM 为 `com.mall:mall:1.0.0-SNAPSHOT`，Java 17、Spring Boot 3.5.13、Spring Cloud / Alibaba 版本由父 POM 管理。
- 依赖现有 `mall-common`，代码包名 `com.mall.cart`，严格保留 `interfaces / application / domain / infrastructure` 四层。
- Spring MVC、Nacos Discovery、MyBatis-Plus 3.5.15、MySQL 8.4、Flyway core + flyway-mysql。
- 注册服务名 `cart-service`，默认端口 `8104`，数据库 `mall_cart`。
- 未实现的目录仅有 `package-info.java`，没有假的业务 Controller / Service / Mapper / RepositoryImpl。

## 2. Flyway 建表（2 张）

| 表名 | 用途 |
| --- | --- |
| `cart` | 一名普通用户唯一的一辆购物车（IAM account_id 逻辑引用） |
| `cart_item` | 购物车里的 SKU 与待购数量（与 cart 建本地 FK） |

由 Flyway 另行生成 `flyway_schema_history`，不计入 2 张业务表。字段、外键、索引与业务边界见 `docs/database-design.md`。

## 3. DDD 包布局

```text
com.mall.cart
├── CartServiceApplication.java
├── interfaces/
│   ├── controller/dto/
│   └── handler/
├── application/cart/
│   ├── command/
│   ├── query/
│   ├── result/
│   ├── port/
│   └── service/
├── domain/cart/
│   ├── model/
│   ├── repository/
│   └── service/
└── infrastructure/
    ├── config/
    ├── client/{iam,product}/
    └── persistence/
        ├── cart/{po,mapper,repository}/
        └── item/{po,mapper,repository}/
```

- Cart 为聚合根，CartItem 为聚合内部实体；将来 Repository 接口放领域层，Mapper/PO/RepositoryImpl 放基础设施层。
- 应用服务未来通过领域 Repository 与外部 Port 编排，**不可直接引用 Mapper/JdbcTemplate**。

## 4. 放进你的 Maven 工程

1. 解压并将 `mall-cart-service/` 放到 `mall-iam-service/` 同级。
2. 在根 `pom.xml` 的 `<modules>` 中添加 `<module>mall-cart-service</module>`（如果已存在，不要重复）。
3. 用有管理权限的 MySQL 账号执行 `docs/create-database.sql` 创建空库。
4. 给购物车服务配置 `CART_DB_URL`、`CART_DB_USERNAME`、`CART_DB_PASSWORD`、Nacos 环境变量；参考 `.env.example`（IDE/Shell 需自己加载，Spring 不会自动读 `.env`）。
5. 启动 `com.mall.cart.CartServiceApplication`，Flyway 自动执行 2 个迁移脚本。
6. 在 MySQL 执行 `docs/verify-schema.sql`，检查 Flyway history 和两张业务表。

根目录构建：

```bash
./mvnw -pl mall-cart-service -am clean package -DskipTests
```

Windows PowerShell：

```powershell
.\mvnw.cmd -pl mall-cart-service -am clean package -DskipTests
```

启动且连接依赖成功后可检查：`http://localhost:8104/actuator/health`。

## 5. 注意事项

- 本期**不会因为加入购物车锁定库存**；SKU 售价、库存、上下架状态在商品/库存服务里维护。
- 由 IAM 的**普通用户账号**拥有购物车，不是商家主体；本库不能通过跨服务 FK 验证账号角色。
- 唯一键不等于完整的幂等和并发控制；需要未来业务实现来保证请求一致性。
- 暂时不支持购物车部分勾选；是否加入该能力需要后续确认业务范围。
- 若旧版 `mall_cart` 已经执行过 Flyway，不能直接重用历史同版本脚本；只能计划新增迁移或在**可销毁的本地库**上重建。
- 真实 Maven 依赖构建、MySQL 8.4 Flyway 迁移、Nacos 注册需在你的环境中验证。
