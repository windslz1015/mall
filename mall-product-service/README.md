# mall-product-service：商品服务初始化版（DDD + Flyway）

> 此包仅包含 Spring Boot 微服务骨架、DDL、状态枚举、DDD 包结构与数据库文档；不包含商品业务接口或 CRUD 实现。

## 1. 已对齐的项目约定

- 父工程：`com.mall:mall:1.0.0-SNAPSHOT`（从 `../pom.xml` 继承）
- Java 17 / Spring Boot 3.5.13 / Spring Cloud 2025.0.3 / Spring Cloud Alibaba 2025.0.0.0，统一由**你现有根 POM** 控制
- 复用 `mall-common`、`Result`、`BusinessException` 等现有基础代码；本包不重新实现
- MyBatis-Plus 3.5.15、Flyway core + MySQL 方言、Nacos Discovery
- `spring.application.name=product-service`，默认端口 `8103`
- 数据库：`mall_product`（独立拥有，不能与其他服务共用 `flyway_schema_history`）

## 2. 四层包结构

```text
com.mall.product
├── interfaces/                  # 未来 Controller / HTTP DTO / Handler
├── application/                 # 商品 / 价格 / 监管用例协调、命令、端口
├── domain/                      # 商品聚合、定价与监管领域规则、Repository 抽象
│   ├── product/model/           # 当前仅状态枚举
│   ├── pricing/
│   └── moderation/
└── infrastructure/              # 未来 MyBatis PO/Mapper/Repository、商家客户端、OSS
    ├── client/merchant/
    └── persistence/
```

初始化时空包使用 `package-info.java`，**没有空 Controller、空 Service 和预写的 SQL 调用逻辑**。业务实现时按需创建，不把 JDBC/Mapper 放在 Application/Domain 层。

## 3. 九张商品业务表

1. `product` 商品基础信息和当前状态
2. `product_spec` 规格项
3. `product_spec_value` 规格值
4. `product_sku` SKU 和当前销售价
5. `product_sku_spec_value` SKU 规格组合
6. `product_image` 图片对象存储 Key
7. `product_price_change_log` 价格变更历史
8. `product_status_log` 上下架历史
9. `product_moderation_log` 平台强制下架记录

完整字段、索引、外键与生命周期说明见 `docs/database-design.md`。

## 4. 放入现有父工程

将整个 `mall-product-service/` 目录放在原有项目根目录，和 `mall-iam-service/`、`mall-merchant-service/` 同级。

检查根 `pom.xml` 的 `<modules>` 中有且仅有：

```xml
<module>mall-product-service</module>
```

本包不需要覆盖既有 `mall-common`、`mall-gateway`、IAM、商家服务任何文件。

## 5. 初始化数据库

1. 使用有权限的数据库管理账号执行 `docs/create-database.sql`，创建空的 `mall_product`。
2. 创建本服务数据库账号并授权（不要使用 root 运行应用；不要在 Git 中写真实密码）。
3. 在本地 IDE / Shell 配置以下环境变量（参见 `.env.example`）：

```text
PRODUCT_DB_URL=jdbc:mysql://127.0.0.1:3306/mall_product?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
PRODUCT_DB_USERNAME=product_app
PRODUCT_DB_PASSWORD=<local-secret>
NACOS_SERVER_ADDR=127.0.0.1:8848
NACOS_USERNAME=nacos
NACOS_PASSWORD=<local-secret>
```

`.env.example` 只是环境变量模板，Spring Boot 默认不会自动导入 `.env` 文件。

4. 确认 MySQL、Nacos 可访问后，启动 `ProductServiceApplication`，Flyway 会按顺序自动运行 9 个迁移。
5. 在 MySQL 上执行 `docs/verify-schema.sql`，核对 9 张业务表及 `flyway_schema_history`。

项目根目录可以执行（前提是父 POM 声明的其他模块也都存在）：

```bash
./mvnw -pl mall-product-service -am clean package -DskipTests
```

若只想启动本模块并有依赖已安装到本地仓库：

```bash
./mvnw -pl mall-product-service spring-boot:run
```

## 6. 初始化阶段的边界

- 不开发创建、修改、价格调整、上下架、查询和监管的真实接口；这是下一阶段的用例实现。
- **不存库存数量**：库存由 `inventory-service` 管理；这里只以 SKU ID 作为跨服务身份。
- `merchant_id` 逻辑引用 `merchant-service` 商家主体，不复制店铺资料，也不在 MySQL 跨库建外键。
- 商品后续变价、变名、下架，不影响 `order-service` 的订单项交易快照。
- 不在初始化阶段接入 RabbitMQ/Seata/复杂缓存；并发和幂等约束将在业务功能设计阶段结合事务实现。
- 需要新库或检查历史：**不要**在已经运行旧迁移的库上直接覆盖 V 文件，否则 Flyway 校验可能失败；当前脚本面向新数据库。

## 7. 验证限制

本包可进行源文件静态检查、Java 17 枚举编译、配置结构校验；真实 Maven 构建和 Flyway MySQL DDL 执行需在你的环境运行。
