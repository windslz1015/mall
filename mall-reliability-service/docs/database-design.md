# mall-reliability-service 数据库设计（仅初始化）

## 1. 服务定位与边界

业务保障域负责**组织可靠性工作**：登记失败、重试编排、补偿调度、定时触发、跨业务域对账与差异恢复。
它不拥有订单、库存、支付单等业务事实。实际动作必须调用业务对象所属服务的**幂等业务接口**，并通过复核确认最终状态。

- 订单超时扫描：可靠性服务记录调度任务和执行过程，由 order-service 判断订单是否仍待支付、是否已超时并进行状态变更。
- 库存释放异常：本服务只记录需要重试的业务意图，库存服务判断 reservation 是否可释放，并完成实际释放。
- 订单已支付状态同步失败：本服务负责安排重试/对账，order-service 按自身业务状态规则处理。支付渠道资金结果以 payment-service 的可靠渠道记录为准。
- RabbitMQ 失败消息：`reliability_message_failure` 仅做集中失败追踪，不替代各服务本地 `outbox_event` / `inbox_event`，也不擅自重发支付等事件。
- 平台管理员异常排查工单属于 admin-service；本库只保存重试任务、对账批次和修复流水。
- 建表并**不**实际启动扫描、执行补偿、连接第三方支付或消费 RabbitMQ，本包没有这些实现。

数据库版本：MySQL 8.4 / InnoDB；主键 `BIGINT UNSIGNED`，时间 `DATETIME(3)`，并发版本 `INT UNSIGNED`。业务键使用 ASCII 二进制排序以保障大小写敏感；跨服务资源 ID 只做逻辑引用，不创建跨库外键。

## 2. 八张 Flyway 业务表

| 迁移 | 表名 | 负责什么 | 关键约束 |
| --- | --- | --- | --- |
| 01 | `reliability_job_definition` | 定时任务配置，例如超时订单扫描、对账扫描；默认 disabled | `job_code` 唯一；CRON 与固定间隔字段互斥；状态和扫描批次约束 |
| 02 | `reliability_job_execution` | 每次扫描/调度的执行记录、时间槽及租约 | `execution_no` 唯一；`(job_id, scheduled_slot_key)` 防重复计划触发；本地 job 外键 |
| 03 | `reliability_recovery_task` | 补偿或消息重试的“意图”、处理状态、次数、下次重试时间 | `task_no` 唯一；`(task_type,business_key)` 稳定去重；尝试次数边界 |
| 04 | `reliability_recovery_attempt` | 补偿每次调用的独立执行流水 | `(recovery_task_id,attempt_no)` 唯一；`request_id` 唯一；本地任务外键 |
| 05 | `reliability_message_failure` | 失败消息的来源、消费者、失败情况及关联补偿任务 | `(producer_service,consumer_key,event_id)` 唯一，不保存原始消息报文 |
| 06 | `reliability_reconciliation_batch` | 订单-支付/订单-库存/渠道支付对账批次及业务时间窗 | `(reconciliation_type,window_start_at,window_end_at,run_round)` 唯一；时间窗约束 |
| 07 | `reliability_reconciliation_difference` | 对账发现的状态不一致明细及关联修复任务 | `(batch_id,difference_key)` 唯一；只能引用本地 batch 和 recovery_task |
| 08 | `reliability_reconciliation_resolution_log` | 对账差异人工/自动处置与复核日志 | `action_no` 唯一；本地差异/任务外键；操作身份约束 |

`flyway_schema_history` 由 Flyway 维护，不算作业务表。MySQL 的 CHECK 限制取值/数量基本合法；**跨行、跨服务并发和业务一致性需要后续在业务代码里落实**。

## 3. 数据关系

```text
reliability_job_definition (1)
    └── (N) reliability_job_execution
                 └── (N) reliability_reconciliation_batch [可选归属]

reliability_recovery_task (1)
    ├── (N) reliability_recovery_attempt
    ├── (N) reliability_message_failure [可选关联]
    ├── (N) reliability_reconciliation_difference [可选关联]
    └── (N) reliability_reconciliation_resolution_log [可选关联]

reliability_reconciliation_batch (1)
    └── (N) reliability_reconciliation_difference (1)
                    └── (N) reliability_reconciliation_resolution_log

订单ID、库存预占ID、支付ID、消息事件ID均为外部逻辑引用，不创建跨库外键。
```

## 4. 主要状态字典（与 `domain/**/model` 枚举一致）

- 计划方式 `ScheduleMode`：`1 CRON`、`2 FIXED_DELAY`。CRON 格式/时区正确性由未来应用层校验。
- 执行状态 `JobExecutionStatus`：`0 QUEUED`、`10 RUNNING`、`20 SUCCEEDED`、`30 FAILED`、`40 TIMED_OUT`、`50 SKIPPED`。
- 恢复任务 `RecoveryTaskStatus`：`0 PENDING`、`10 RUNNING`、`20 RETRY_WAIT`、`30 SUCCEEDED`、`40 EXHAUSTED`、`50 CANCELLED`。
- 恢复尝试 `RecoveryAttemptStatus`：`0 STARTED`、`10 SUCCEEDED`、`20 FAILED`、`30 TIMED_OUT`。
- 失败消息 `MessageFailureStatus`：`0 OPEN`、`10 RETRY_SCHEDULED`、`20 RECOVERED`、`30 IGNORED`。
- 对账批次 `ReconciliationBatchStatus`：`0 PENDING`、`10 RUNNING`、`20 COMPLETED`、`30 FAILED`、`40 CANCELLED`。
- 差异处理 `DiscrepancyStatus`：`0 OPEN`、`10 RECOVERY_PENDING`、`20 RESOLVED`、`30 IGNORED`。
- 优先级/严重度：`1 LOW`、`2 NORMAL`、`3 HIGH`、`4 CRITICAL`。

`job_definition.enabled=0`：未来没有实现调度器时，也不会存在任何自动执行。

## 5. 幂等、并发与日志原则（只设计约束，不声称已有运行能力）

1. **调度去重**：同一任务同一个 `scheduled_slot_key` 唯一。人工触发该键为 NULL，可产生多条不同执行；通过 `execution_no` 追踪。
2. **任务意图去重**：`(task_type,business_key)` 唯一，例如 `INVENTORY_RELEASE` + `order:123:item:456:release`。不能只用 `order_id` 作为所有补偿类型的去重键。
3. **重试可控**：存储 `attempt_count`、`max_attempts`、`next_execute_at`、租约字段、乐观锁版本与逐次尝试历史；这些字段本身不自动保障多实例并发，需要应用层原子领取/到期重领。
4. **消息失败定位**：按事件生产方、消费处理器和事件 ID 去重。它记录“哪里处理失败”，实际重新消费应由原消费者或其明确提供的幂等重试入口执行，不应在集中服务盲目重放原始消息。
5. **对账批次重跑**：一组类型、时间窗加 `run_round` 唯一；重新对账应增加轮次，不要覆盖已有差异历史。窗口采用 `[start,end)` 避免时间边界重复/遗漏。`difference_count` 统计差异条目数，一个对象可能命中多条规则，因此不约束其必须小于 `scanned_count`。
6. **差异解决并非仅改状态**：对账差异关联补偿任务后，还要调用业务拥有者查询最终真实状态，确认一致后才能标为 RESOLVED；忽略必须留处置日志和理由。
7. **最小权限与隐私**：只有本地表之间建外键；日志只保留脱敏错误摘要，`context_json` 不得记录支付密钥、Token、个人敏感信息、完整回调或请求报文。
8. **任务/日志保留**：执行流水和异常差异增长较快，未来需要归档/保留策略、相关索引审查与告警机制；本版不假定这些机制已实现。

## 6. 不属于本次初始化的功能

- 不创建实际的 `@Scheduled`、Quartz、XXL-JOB 执行器或 RabbitMQ 消费者。
- 不写任何订单超时、库存释放、支付状态同步、消息重试、异常补偿或业务对账业务代码。
- 不注入跨服务 OpenFeign 客户端、Seata 全局事务、Redis 锁、RabbitMQ Listener。
- 不复制其他服务的订单、支付、库存核心表；不额外创建中心化 Outbox/Inbox 以取代原业务域的可靠消息机制。

## 7. 数据库迁移约定

- 先由 DBA 执行 `docs/create-database.sql` 创建**空** `mall_reliability` 库。
- Flyway 自动执行 `V2026110301` 至 `V2026110308`；配置 `baseline-on-migrate=false`、`validate-on-migrate=true` 和 `clean-disabled=true`。
- 初始化完成可运行 `docs/verify-schema.sql` 检查表、迁移历史、外键及唯一索引。
- 不要把旧版非 DDD 服务和本目录合并；不要直接把 SQL 执行到已有数据的非空库。
- Flyway 脚本成功执行后不得修改内容。后续调整使用新增 `V...` 版本的迁移脚本。
