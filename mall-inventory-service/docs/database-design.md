# 库存微服务数据库设计（DDD + Flyway，项目初始化阶段）

## 1. 业务边界

- 本服务唯一拥有 `mall_inventory` 数据库，负责 **SKU 库存余额、预占锁定、确认扣减、取消释放、库存调整、库存流水以及成功操作幂等记录**。
- `product-service` 负责 SKU 是否存在、商品及商家的可售条件；库存服务不持有 SPU/SKU 定价、标题及上下架状态。
- `inventory_stock.sku_id` 引用商品服务的 `product_sku.id`；`product_id`、`merchant_id` 也是跨服务**逻辑引用**，不建立跨库外键。使用本服务的 ID 不能绕过实际 SKU 归属/商家权限校验。
- `order_id`、`sub_order_id`、`order_item_id` 为订单服务的逻辑引用，本库不复制订单快照或订单金额。
- `operator_account_id` 是 IAM 账号主键的逻辑引用；**商家账号与商家主体仍为不同对象**。
- `reliability-service` 负责协调跨服务补偿任务，不能直接更新本库。业务状态更新必须通过库存服务执行。

## 2. 六张业务表

| 表名 | 核心字段 | 业务用途 |
| --- | --- | --- |
| `inventory_stock` | sku_id、product_id、merchant_id、on_hand_qty、available_qty、reserved_qty、version | 一个 SKU 一条当前库存余额 |
| `inventory_reservation` | reservation_no、inventory_id、order_id、sub_order_id、order_item_id、quantity、status、expires_at | 订单明细级预占及状态 |
| `inventory_operation` | operation_no、operation_type、source_service、request_key、request_fingerprint、inventory_id、reservation_id | 成功库存变更的幂等记录，唯一键约束重复执行 |
| `inventory_adjustment` | operation_id、quantity_delta、reason_code、operator_type、operator_account_id | 补货/盘点等库存调整审计 |
| `inventory_stock_ledger` | operation_id、库存前后三组数量、三个 delta | 所有库存变更的永久流水 |
| `inventory_reservation_event` | reservation_id、operation_id、from_status、to_status、reason_code | 锁定/确认/释放状态流转历史 |

Flyway 自身创建 `flyway_schema_history`；不计入六张业务表。

## 3. 库存数量定义（必须统一）

- `on_hand_qty`：当前实有库存，包含已经锁定尚未扣减的数量。
- `available_qty`：可供新订单锁定的数量。
- `reserved_qty`：已被有效订单锁定，等待支付确认或释放的数量。
- **不变式：** `on_hand_qty = available_qty + reserved_qty`；DDL 中提供 `CHECK` 约束。
- 不在余额表存累计销量；销量与支付历史属于其他领域或统计能力，不影响当前库存一致性。

以初始实有 10、订单预占 2 为例：

| 动作 | on_hand | available | reserved | 说明 |
| --- | ---: | ---: | ---: | --- |
| 初始 | 10 | 10 | 0 | 都能参与新交易 |
| 锁定 2 | 10 | 8 | 2 | 预占库存，不会立即减实有 |
| 支付确认扣减 2 | 8 | 8 | 0 | 扣实有和预占，**可用不再减一次** |
| 若改为取消：释放 2 | 10 | 10 | 0 | 从“锁定”分支释放，不叠加在支付确认之后 |

调整库存时原则上只改 `on_hand_qty` 和 `available_qty`，保留已经预占的 `reserved_qty`；如果扣减导致 `available_qty < 0`，必须拒绝该调整。

## 4. 预占生命周期

`inventory_reservation.status`：

- `1=LOCKED`：新订单占用库存，等待支付/取消。
- `2=CONFIRMED`：订单有效支付，锁定库存最终扣减。
- `3=RELEASED`：取消、超时或交易异常，锁定库存释放。

只允许以下迁移：

```text
NULL --预占成功--> LOCKED
LOCKED --支付成功确认--> CONFIRMED
LOCKED --取消/超时释放--> RELEASED
```

不存在 `CONFIRMED -> RELEASED` 普通路径。两种最终状态互斥；支付与取消竞争必须在未来业务实现中通过**条件更新或行锁**解决。业务表上的 CHECK 与唯一约束不能自动完成并发状态机。

`expires_at` 表示预占有效期，不意味着数据库自行超时释放。需要未来定时任务调用库存服务的合法释放用例，且必须和订单状态裁决一致。

## 5. 幂等与跨业务一致性

- `inventory_reservation.order_item_id` 唯一：同一订单明细不能产生多条预占记录（即使调用重试）。
- `inventory_operation(source_service, operation_type, request_key)` 唯一：同一个来源和同一类型的相同请求键只保留一条**已成功提交**的操作。
- `request_fingerprint` 记录规范化请求体 SHA-256。相同 key 但不同参数必须拒绝；**数据库本身不会计算或校验这个摘要**。
- `inventory_stock_ledger.operation_id` 唯一、`inventory_adjustment.operation_id` 唯一、`inventory_reservation_event.operation_id` 唯一，防止同一次操作出现重复的相应业务记录。
- **关键写操作必须在同一个 MySQL 本地事务中提交**：余额条件更新、预占及状态更新、库存操作记录、库存变更流水、预占事件/调整审计。任何一步失败应回滚全部。不能只靠插幂等表或 Flyway 保证事务正确性。
- 操作记录本身代表最终提交成功的库存变更，因此业务失败时应该回滚它；不在该表里存异步补偿任务的执行进度。
- `inventory_stock.version` 和 `inventory_reservation.version` 为后续实现乐观锁/条件更新预留，不代表目前已经防超卖。
- 初始库存设置和后续补货也应产生调整记录和流水；**不推荐在业务层直接裸改余额表**。

## 6. 表间关系

```text
product-service: product_sku.id
     (逻辑 SKU ID，不建立跨库外键)
           |
           v
inventory_stock [余额]
     | 1:N inventory_reservation [订单明细预占]
     | 1:N inventory_operation [成功操作幂等]
     |         | 1:1 inventory_stock_ledger [数量流水]
     |         | 0:1 inventory_adjustment [调整操作]
     |         + 0:1 inventory_reservation_event [预占状态事件]
     |
     + 1:N inventory_stock_ledger

order-service: order / sub_order / order_item
     (逻辑 ID -> inventory_reservation)
```

库存内部 FK 使用 `RESTRICT` 避免误删历史流水。复合 FK（operation+stock、reservation+stock）尽可能约束同一个 SKU 的记录不能串库存；跨微服务一致性仍需要 API 校验和业务测试。

## 7. 索引与表设计注意事项

- `inventory_stock.sku_id` 唯一：同一 SKU 不可出现两条库存余额。
- 余额表 `(merchant_id, id)` 支持商家批量库存查询；`(product_id, id)` 支持按商品查询关联 SKU 库存。
- 预占表 `(order_id, status, id)` 和 `(sub_order_id, id)` 支持订单查询；`(status, expires_at, id)` 为未来过期预占排查准备索引，但**是否能释放必须取决于订单服务状态**。
- 流水 `(inventory_id, created_at, id)` 和 `(reservation_id, created_at, id)` 支持审计追溯。
- 三组数量统一 `BIGINT UNSIGNED`；增减量使用有符号 `BIGINT`；对极大值溢出和并发更新仍需业务层保护。
- 所有服务内主键为 `BIGINT UNSIGNED`；状态码为 `TINYINT UNSIGNED`；时间使用 `DATETIME(3)`。
- 订单明细 `order_item_id` 假设在订单服务中全局唯一；如果未来定义为订单内局部编号，应把唯一约束改成 `(order_id, order_item_id)`，并通过新增 Flyway migration 演进。

## 8. 不在此次初始化范围

- 具体库存调整、查库存、锁库存、确认、释放 HTTP API、消费者、定时任务、领域行为实现。
- MyBatis PO、Mapper、RepositoryImpl、RestClient、OpenFeign 实际调用。
- RabbitMQ 消息可靠投递、Outbox、Seata、Redis 分布式锁或补偿调度平台。是否引入这些能力要在业务场景开发时再确认。
- 库存批次、仓库维度、冻结库存、预售、跨币种/跨境仓、库存结算与退货回补。当前需求仅明确**每 SKU 一份可用、锁定的库存**。

## 9. 迁移顺序及历史数据库处理

1. `V2026110301__create_inventory_stock.sql`
2. `V2026110302__create_inventory_reservation.sql`
3. `V2026110303__create_inventory_operation.sql`
4. `V2026110304__create_inventory_adjustment.sql`
5. `V2026110305__create_inventory_stock_ledger.sql`
6. `V2026110306__create_inventory_reservation_event.sql`

**只适用于全新空的 `mall_inventory`。** 如果之前旧版 inventory-service 已经运行过 Flyway，不要直接覆盖已执行的 `V...sql`；应根据 `flyway_schema_history` 规划增量迁移，或仅在确认可销毁的本地测试库上重建新库。
