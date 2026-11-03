# mall-order-service 数据库设计（只初始化，不做业务）

## 1. 本服务的业务边界

- 订单服务**拥有**：用户主订单、按商家拆分的子订单、订单项不可变成交快照、订单状态、取消记录、关键操作幂等记录，以及订单侧 Outbox/Inbox 可靠消息记录。
- IAM **拥有**：普通用户账号、商家账号与权限。`buyer_account_id` / `actor_account_id` 只保留 IAM 账号 ID。
- merchant-service **拥有**：商家经营主体；`merchant_id` 是商家主体 ID，**不是商家账号 ID**。商家名称/业务编号在子订单中以快照留存。
- product-service **拥有**：SPU、SKU、现价、上下架。订单保存商品名称、规格、单价等成交快照，商品改价/改名不更新历史订单。
- inventory-service **拥有**：SKU 实有/可用/锁定数量、订单明细级预占状态、库存流水。**不在订单库建库存表**。
- payment-service **拥有**：支付单、第三方支付回调、支付记录与支付渠道状态。订单库只记录被确认后的订单状态变化，不保存第三方支付凭证。
- reliability-service 日后可调度超时扫描和跨服务补偿，但应由 order-service 自己判断并执行订单状态转移。

## 2. 业务表（9 张）

| 表 | 所属业务职责 | 主要约束 / 索引 |
| --- | --- | --- |
| `trade_order` | 用户一次提交形成的交易主订单、应付金额与支付截止时间 | 全局唯一 `order_no`；`buyer_account_id` 查询索引；`(status, pay_deadline_at, id)` 超时扫描索引 |
| `trade_sub_order` | 主订单按商家拆分后的子订单 | 唯一 `(order_id, merchant_id)`；商家维度索引；同库父订单 FK |
| `trade_order_item` | 子订单的 SKU 明细与成交快照 | 唯一 `(sub_order_id, sku_id)`；组合 FK 约束项不能挂错父订单 |
| `trade_order_operation` | 提交/取消/超时/支付结果应用/补偿的关键操作幂等记录 | 唯一 `(source_service, operation_type, request_key)`；请求指纹 |
| `trade_order_cancel_record` | 用户、超时及系统取消的最终成功记录 | 一笔主订单最多一条最终取消记录；绑定幂等操作 |
| `trade_order_status_log` | 主订单状态流转历史 | 仅追加，不替代主订单当前状态 |
| `trade_sub_order_status_log` | 商家子订单状态流转历史 | 仅追加，不替代子订单当前状态 |
| `outbox_event` | 订单变更事务性待发送事件 | 唯一 `event_id` / `biz_key`；`claim_token` 用于投递租约 |
| `inbox_event` | 订单消费方幂等收件记录 | 唯一 `(consumer_name, event_id)`；必须检查事件负载指纹 |

`flyway_schema_history` 是 Flyway 自动创建的版本历史表，**不计入九张业务表**。

## 3. 主订单 / 子订单 / 订单项关系

```text
iam-service.iam_account.id                （跨服务逻辑引用）
             |
             v
     trade_order (主订单) 1 ------------- N trade_sub_order (商家子订单)
        |                                      |      |
        |                                      |      '-- merchant_id -> merchant-service.merchant.id
        |                                      |
        |                                      1
        |                                      |
        |                                      N
        |                               trade_order_item
        |                                      |
        |                                      '-- sku_id/product_id -> product-service
        |
        +---- trade_order_operation (幂等操作)
        +---- trade_order_cancel_record (最终取消)
        +---- trade_order_status_log (状态历史)

trade_order_item.id --> inventory-service.inventory_reservation.order_item_id
trade_order.id      --> payment-service 支付单的订单引用
```

子订单唯一键 `(order_id, merchant_id)` 表示“一次提交里同一商家只产生一个商家子订单”；
子订单的 `merchant_name_snapshot`、`merchant_no_snapshot` 不随商家资料修改而变化。

订单项通过组合外键 `(sub_order_id, order_id) -> trade_sub_order(id, order_id)` 确保不会把子订单项关联到另一个主订单。
不能通过数据库 FK 验证 `sku_id` 是否属于该商家，未来需调用商品/商家服务校验并保存成交快照。

## 4. 状态数据字典

**主订单/子订单状态**（对应 Java `OrderStatus` / `SubOrderStatus`）：

- `0 CREATING`：内部创建中，涉及交易校验和库存预占过程；外部不可直接支付。
- `10 PENDING_PAYMENT`：下单与库存预占完成，允许发起支付。
- `20 PAID`：支付服务确认支付成功，订单进入当前项目的支付完成态。
- `30 CANCELLED`：由用户主动取消或系统在截止后取消；必须配合库存释放。
- `40 CREATION_FAILED`：创建阶段失败、已开始执行必要的库存回滚/补偿；不是取消已支付订单。

**订单取消类型**：`1 USER`，`2 PAYMENT_TIMEOUT`，`3 SYSTEM_EXCEPTION`。

**幂等操作类型**：`1 SUBMIT`，`2 USER_CANCEL`，`3 TIMEOUT_CANCEL`，
`4 APPLY_PAYMENT_SUCCESS`，`5 APPLY_PAYMENT_FAILURE`，`6 COMPENSATE`。

**幂等操作状态**：`1 PROCESSING`，`2 SUCCEEDED`，`3 FAILED`。

**Outbox**：`PENDING` / `SENDING` / `SENT` / `FAILED`。
**Inbox**：`PROCESSING` / `PROCESSED` / `FAILED`。

当前只设计了“支付及取消”阶段，不包括发货、收货、售后、退款或部分商家单独支付；后续若有这些业务必须新设计状态机并新增 Flyway 迁移。

## 5. 金额与交易快照

- 货币金额使用 `DECIMAL(18,2)`，数据库不使用浮点数。
- `trade_order_item.line_amount = unit_price * quantity` 用 CHECK 约束。
- 每个子订单的 `subtotal_amount` 应等于其订单项金额之和；主订单 `item_total_amount` 应等于子订单金额之和。**跨行汇总无法用 MySQL CHECK 实现**，后续在应用层以数据库事务保障。
- 当前需求没有优惠券、运费、折扣，所以 `trade_order.payable_amount = item_total_amount` 由 CHECK 约束。未来新增促销时，应先设计金额组成并新增迁移，不得直接修改旧版 SQL。
- 订单项快照保存商品名、SKU 名称/业务编号、规格 JSON（无规格时使用 `[]`）、价格、数量、成交金额及商品图片对象 Key。商品后续变化不得影响订单历史。
- `trade_order_item` 的 SKU ID 逻辑引用商品服务，不建立跨库 FK。

## 6. 超时取消与支付结果并发

- 只在 `trade_order` 保存 `pay_deadline_at`，并建立 `(status, pay_deadline_at, id)` 索引供定时扫描。**不另建一份重复的超时任务状态表**。
- 用户取消与系统超时取消都只允许从 `PENDING_PAYMENT(10)` 状态转到 `CANCELLED(30)`；`PAID(20)` 不能被普通取消。
- 订单支付确认只能从 `PENDING_PAYMENT(10)` 转到 `PAID(20)`；两种并发操作必须用带旧状态和版本条件的更新或行锁完成竞争裁决。
- 如果订单已取消却收到第三方支付成功事实，不能改写成正常的已支付订单；应由支付域触发资金异常处理/退款补偿，本次没有实现退款。
- 支付成功后若订单更新失败，必须保留支付事实并重试/对账恢复；单纯写入 Outbox/Inbox 表不自动提供可靠性。
- 状态日志、取消记录、主订单、子订单状态应使用同一个**订单库本地事务**写入；跨库存和支付的操作必须另做可靠消息/补偿。

## 7. 与库存预占模型对齐

已初始化的 inventory-service 表 `inventory_reservation` 有 `order_id`、`sub_order_id`、`order_item_id` 三个业务标识：分别指向本库 `trade_order.id`、`trade_sub_order.id`、`trade_order_item.id`。
因此未来库存锁定的顺序必须先能得到这些 ID；可先在订单库以 `CREATING` 状态提交订单骨架与订单项，远程锁定成功后再转入 `PENDING_PAYMENT`。如果部分 SKU 锁定失败，要释放已锁定部分，按失败/补偿规则更新订单状态。
库存的 `LOCKED` / `CONFIRMED` / `RELEASED` 状态及实际库存数量完全归 inventory-service，不要再在订单库维护第二份可写的库存数量。

## 8. 幂等、Outbox / Inbox

- `trade_order_operation` 使用 `(source_service, operation_type, request_key)` 唯一键，必须核对 `request_fingerprint` 防止重复键对应不同请求。`request_key` 必须由可靠调用方提供，不能每次重试都随机生成。
- 相同订单状态修改、状态日志、幂等结果必须在同一个订单库事务里写入；外部库存/支付调用不会因此自动事务化。
- `outbox_event` 作为**未来**本地事务事件投递的持久化容器，字段结构沿用 IAM 的核心设计（含 claim_token）；没有投递线程和 RabbitMQ 功能。
- `inbox_event` 未来可用于处理重复支付/库存事件：消费去重记录应与订单状态修改在同一事务写入。失败时需要支持重试，并检查事件指纹。
- `trade_order_status_log` 和 `trade_sub_order_status_log` 记录状态历史，不是对账结果的唯一来源。

## 9. Flyway 顺序

1. `V2026110301__create_trade_order.sql`
2. `V2026110302__create_trade_sub_order.sql`
3. `V2026110303__create_trade_order_item.sql`
4. `V2026110304__create_trade_order_operation.sql`
5. `V2026110305__create_trade_order_cancel_record.sql`
6. `V2026110306__create_trade_order_status_log.sql`
7. `V2026110307__create_trade_sub_order_status_log.sql`
8. `V2026110308__create_outbox_event.sql`
9. `V2026110309__create_inbox_event.sql`

**脚本只用于新建空库**。已执行过的 Flyway `V` 版本不能直接覆盖，已有旧表也不能指望 `baseline-on-migrate=false` 自动兼容。后续通过新增迁移升级。

## 10. 尚未实现的业务范围

只提供 DDD 包骨架、启动类、POM、配置、五个轻量状态/操作类型枚举和九个 Flyway 建表文件。
**没有** Controller、Application Service、仓储接口/实现、PO/Mapper、状态机、真实支付、RabbitMQ 消息投递/消费、超时调度、库存联调、Seata 或业务测试接口。
实际实现时要先确认：支付截止时间、是否需要部分下单、主订单与支付单关系、支付失败是否可重试、支付成功和取消冲突处理策略。
