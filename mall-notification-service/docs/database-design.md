# mall-notification-service 数据库设计

## 1. 领域边界

- `iam-service` 负责账号注册、账号归属和注册成功事件；通知服务不读取/修改 IAM 数据库。
- `notification-service` 负责将 `AccountRegistered` 事件转换成可查询的站内欢迎通知，不创建账号、不发送真实短信或电子邮件。
- 通过 RabbitMQ `iam.events` 的路由键 `account.registered` 订阅事件，专用队列为 `notification.account.registered`。
- 跨服务只存 IAM 的 `account_id` 逻辑引用，不建跨库外键。

## 2. 表设计

| 表 | 作用 | 关键约束 |
| --- | --- | --- |
| `notification_message` | 用户站内消息、已读状态 | `biz_key` 唯一；索引 `(account_id, created_at, id)` |
| `inbox_event` | RabbitMQ 接收事件幂等 | `(consumer_name, event_id)` 唯一，包含SHA-256内容指纹 |

## 3. 一致性和失败处理

1. IAM的账号与IAM的Outbox在一个本地事务提交；IAM异步向RabbitMQ发送`ACCOUNT_REGISTERED`事件。
2. 本服务解析IAM发布的原始JSON事件（`eventId/accountId/accountNo/username/accountType/occurredAt`），核对消息头和事件ID。
3. 仅 `accountType=1` 的普通用户生成欢迎通知；同一个消费事务内插入Inbox记录和站内消息。
4. `(consumer_name,event_id)` 防止同一事件重复消费，`biz_key=WELCOME_REGISTERED:{accountNo}` 防止重复注册事件生成多条欢迎通知。
5. 数据库提交后listener正常返回，由Spring AMQP自动ACK。异常经过有限重试后进入死信队列 `notification.account.registered.dlq`，需运营排查并受控重放。
6. 不与IAM数据库构建分布式事务；跨服务采用最终一致性。

## 4. 不实现的范围

短信、邮件、微信模板消息、推送供应商API、通用通知模板管理、多渠道发送状态、定时推送均不在初始化版本中实现。以后接入时，需要增加独立渠道发送任务、失败重试和服务商凭据管理。
