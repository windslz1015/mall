# mall-payment-service 数据库设计（仅初始化）

## 1. 边界与约定

- **支付域拥有**：支付单、每次支付渠道尝试、真实支付入账事实、渠道通知接收记录、幂等操作记录、支付状态日志、支付侧 Outbox / Inbox。
- **交易域拥有**：交易主订单、商家子订单、成交金额、订单状态和支付截止时间。支付库只存 `trade_order_id`、`trade_order_no` 等跨域引用/必要快照，不修改订单库。
- **账号域拥有**：用户账号。`buyer_account_id` 是 `iam-service` 的账号 ID，不是商家主体 ID。
- **商家域拥有**：商家主体，支付不在本期进行商家拆账/结算，**不新建商家余额或账务表**。
- 本期仅支持“**一笔交易主订单 -> 一笔支付单 -> 多次支付尝试**”，统一整单支付，金额大于零、币种 CNY；部分支付、退款、结算、优惠分摊均不在本期范围。
- MySQL 8.4 / InnoDB，主键 `BIGINT UNSIGNED`，金额 `DECIMAL(18,2)`，时间 `DATETIME(3)`。跨数据库/跨服务引用 **不建 FK**。

## 2. 八张表

| 表 | 意义 | 关键规则 |
| --- | --- | --- |
| `payment_order` | 一个订单对应一个平台支付单 | `payment_no` 与 `trade_order_id` 均唯一；金额大于零；保存订单截止时间快照 |
| `payment_attempt` | 一笔支付单可多次尝试支付渠道 | `(payment_order_id, attempt_no)`、`request_no` 唯一；单次请求金额大于零 |
| `payment_record` | 渠道**已确认成功扣款**的事实记录 | `(channel_code, channel_trade_no)` 唯一；不对 `payment_order_id` 唯一，保留重复真实扣款事实以供复核 |
| `payment_notification` | 第三方支付 HTTP 通知及验签处理状态 | `(channel_code, notification_key)` 唯一；只存规范化指纹，不保存完整敏感原始报文 |
| `payment_operation` | 创建/发起/回调结果处理/关闭/对账幂等 | `(source_service, operation_type, request_key)` 唯一并校验内容指纹 |
| `payment_status_log` | 支付单状态变化历史 | 追加写，引用支付单，可选引用幂等操作 |
| `outbox_event` | 支付状态改变后的可靠事件投递占位 | `event_id` / `biz_key` 唯一，claim token + 重试字段；仅建表 |
| `inbox_event` | 消费订单等 RabbitMQ 事件时去重占位 | `(consumer_name, event_id)` 唯一；**不等于** HTTP 回调记录表 |

`flyway_schema_history` 由 Flyway 自动建立，不计入八张业务表。

## 3. 关系

```text
order-service.trade_order.id   IAM.iam_account.id
            |                        |
            +-----------+------------+
                        | 逻辑引用（不建跨库 FK）
                        v
               payment_order
                 1    |    1
                 |    |    |---- N payment_status_log
                 |    |    |---- N payment_operation
                 |    |    |---- N payment_notification（匹配成功的回调）
                 |    |
                 N    N
          payment_attempt   payment_record
                 |                  |
                 +----可选关联-------+

          outbox_event / inbox_event （支付本地消息容器）
```

`payment_record` 的 `(payment_attempt_id, payment_order_id)` 组合外键保证**如果填写支付尝试，则不能关联另一支付单的尝试**；若未知对应尝试，可以为 NULL。渠道的真实支付成功通知可能在订单取消后到达，入账事实仍需记录并进入异常复核。

## 4. 状态字典与快照

### payment_order.status（Java `PaymentStatus`）

- `0 CREATED`：支付单已创建，还未发起有效渠道支付。
- `10 PENDING`：存在进行中的支付尝试或等待渠道结果。
- `20 SUCCEEDED`：渠道成功结果经验证，支付单被确认成功；设置 `paid_at`。
- `30 FAILED`：支付流程失败，本期是否允许重试由业务规则另行确定。
- `40 CLOSED`：支付截止/订单取消后，完成支付关闭；设置 `closed_at`。
- `50 EXCEPTION`：资金结果与订单状态冲突或需要进一步人工/自动对账；不是简单成功或失败。

### payment_attempt.status（Java `PaymentAttemptStatus`）

`0 CREATED` / `10 PROCESSING` / `20 SUCCEEDED` / `30 FAILED` / `40 CLOSED`。

### payment_notification.process_status（Java `PaymentNotificationStatus`）

`0 RECEIVED` / `10 VERIFIED` / `20 PROCESSED` / `30 REJECTED` / `40 FAILED`。通知必须先验证渠道签名（必要时主动查单确认），不能仅凭回调 JSON 标记支付成功。

### payment_operation.status（Java `PaymentOperationStatus`）

`1 PROCESSING` / `2 SUCCEEDED` / `3 FAILED`。重复幂等键应比对 `request_fingerprint`，不允许将不同业务请求误当同一个请求。

### payment_record.review_status（Java `PaymentReviewStatus`）

`0 NORMAL` / `10 LATE_SUCCESS` / `20 DUPLICATE_SUCCESS` / `30 AMOUNT_MISMATCH` / `40 NEEDS_REVIEW`。

## 5. 并发 / 退款 / 对账边界

- `payment_order` 与交易主订单一对一是一项**当前版本建模决策**。失败后重试使用 `payment_attempt`，不能简单重复创建支付单。
- 数据库唯一索引可以去重支付回调和渠道交易记录，但**不能单凭这些索引保证没有重复扣款**。未来实现需结合渠道幂等请求号、订单状态条件更新与对账。
- “两个不同渠道交易号都真实扣款成功”属于资金事实，`payment_record` 允许存两行，不应因 `payment_order_id` 唯一约束而丢掉第二笔。后续明确退款流程后才能正确处理多扣资金。
- 回调验签结果只能说明消息可靠，仍需要核验平台支付单号、币种、金额、渠道交易号和渠道交易状态；无法匹配的通知保留在 `payment_notification`。
- 订单取消/支付超时后收到成功通知，不能静默把订单从已取消改回已支付。支付单可进入 `EXCEPTION`，留待退款/异常补偿；退款能力**本期未实现**。
- `payment_order.payable_amount` 必须与 `trade_order.payable_amount` 一致，但**跨库 CHECK 做不到**，由未来的应用服务在支付发起前向订单域校验。
- 每次支付尝试金额等于支付单应付金额、实付记录金额的异常判定等，涉及跨行/跨表验证，由未来的本地事务和业务规则完成。
- 支付结果成功事件应在支付域本地事务里写 Outbox 后再异步通知订单域。订单域自己更新订单状态；支付服务不跨库改订单表。
- 支付结果持久化成功不代表跨服务最终一致；仍需要可靠消息、订单域 Inbox 和周期性对账/补偿。
- 表结构并不自动实现状态机、并发处理、回调鉴权、RabbitMQ 或外部支付 SDK。

## 6. 迁移脚本及管理

1. `V2026110301__create_payment_order.sql`
2. `V2026110302__create_payment_attempt.sql`
3. `V2026110303__create_payment_record.sql`
4. `V2026110304__create_payment_notification.sql`
5. `V2026110305__create_payment_operation.sql`
6. `V2026110306__create_payment_status_log.sql`
7. `V2026110307__create_outbox_event.sql`
8. `V2026110308__create_inbox_event.sql`

Flyway 只应针对**新空库**第一次执行。旧版本库已存在 `flyway_schema_history` 时不能覆盖旧迁移，应通过新 V 文件升级。`baseline-on-migrate=false` 避免将未知旧库静默接管。

## 7. 本次明确不实现

无 Controller / Service / RepositoryImpl / Mapper / PO / RabbitMQ 生产与消费 / OpenFeign 客户端 / 第三方支付接口 / 支付回调验签 / 对账 / 退款。
保留 DDD 包结构、五个轻量状态枚举、八张表和运行配置。对于待定业务规则只在本文记录，不臆造实际功能。
