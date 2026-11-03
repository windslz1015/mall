# mall-admin-service 数据库设计（仅初始化）

## 1. 服务业务边界

平台运营服务是**后台监管的业务入口与自有监管数据拥有者**，不是全部业务数据的中央仓库。

- **IAM 服务**：普通用户与平台管理员账号、登录、角色权限和用户账号状态；本库只存管理员 `account_id` 逻辑引用。
- **Merchant 服务**：商家入驻审核及商家启用禁用的真实状态与变更记录；本库仅记录监管案件、处置请求及操作审计。
- **Product 服务**：商品上/下架真实状态、平台强制下架流水；本库不会重复存一份商品上下架状态。
- **Order 服务**：用户订单与商家子订单；后台要查询时调用订单域的受权接口，不复制订单。
- **Payment 服务**：支付单、资金事实和支付记录；后台查询时调用支付域的受权接口，不复制支付数据。
- **Reliability 服务**：重试/对账/异常补偿任务状态；本库只做异常排查工单，不保存真实补偿任务及其执行结果。

本期监管案件的创建、审核、处置仅做**数据结构预留**。当前没有查询、审核、强制下架等业务实现。

MySQL 8.4 / InnoDB，ID 为 `BIGINT UNSIGNED`，时间 `DATETIME(3)`，`version INT UNSIGNED`。跨服务/跨数据库关联仅存业务 ID，**不建立跨库外键**。创建/状态变更由将来 application 层协调、domain 层验证、infrastructure 层持久化。

## 2. 七张 Flyway 表

| 表名 | 自有业务事实 | 关键约束及查询入口 |
| --- | --- | --- |
| `admin_operation_log` | 管理员操作审计，包含管理类敏感查询事件 | `operation_no` 唯一；按管理员+时间、动作+时间、资源、Trace ID 查询；不保留密码、Token、完整个人资料 |
| `admin_regulation_case` | 一起监管案件的标题、类别、状态、处理人 | `case_no` 唯一；按状态/优先级/处理人查询；`version` 为未来并发控制预留 |
| `admin_case_target` | 案件关联 IAM/商家/商品/订单/支付业务对象 | `(case_id,target_domain,target_type,target_id)` 唯一；只 FK 本地 `admin_regulation_case` |
| `admin_case_action` | 管理人员要求下游业务执行的处置动作及其结果 | `action_no` 唯一，可作为下游幂等键；组合 FK 保证动作关联对象与案件一致 |
| `admin_case_status_log` | 监管案件状态变化历史 | 追加写；支持管理员或系统触发 |
| `admin_exception_investigation` | 平台异常排查单、归属资源、处理进度 | `investigation_no` 与 `detection_key` 唯一；可选择关联监管案件 |
| `admin_investigation_log` | 排查单的分派、备注和状态变化历史 | 追加写；内部 FK 指向排查单 |

`flyway_schema_history` 由 Flyway 创建，不计入七张业务表。

## 3. 业务对象关系

```text
      IAM.iam_account.id（只做逻辑引用）
                        |
             监管人员 / 被指派人员
                        |
               admin_regulation_case
                  |  1            1 |
                  |                 +---- N admin_case_status_log
                  N
             admin_case_target
                  | 1
                  N
             admin_case_action    (通过 case_id + target_id 组合 FK 校验同案)

               admin_exception_investigation
                  | 1           | (可选)
                  N             v
           admin_investigation_log  admin_regulation_case

               admin_operation_log （独立的审计流水）

 外部目标对象ID: IAM / MERCHANT / PRODUCT / ORDER / PAYMENT
 全部是跨服务逻辑引用，禁止在 admin 库添加对应业务表或跨库 FK。
```

## 4. 字段/状态字典

### `admin_operation_log.outcome` / `AuditOutcome`

- `0 STARTED`：请求开始处理。
- `10 SUCCEEDED`：操作成功。
- `20 FAILED`：操作失败。

操作日志是**审计记录**，不能替代真正的业务操作幂等表。尽量为敏感信息查询也留审计事件；日志内容需要脱敏和合规保留周期。

### `admin_regulation_case.status` / `RegulationCaseStatus`

`0 OPEN`、`10 IN_REVIEW`、`20 ACTION_PENDING`、`30 RESOLVED`、`40 CLOSED`、`50 DISMISSED`。

- `CLOSED` 和 `DISMISSED` 必须填写 `closed_at`。
- `priority` 值：1 LOW、2 NORMAL、3 HIGH、4 CRITICAL。
- 状态变更日志放 `admin_case_status_log`，商品或商家真实状态日志仍在其自有服务。

### `admin_case_target`

`target_domain` 限制为 `IAM`、`MERCHANT`、`PRODUCT`、`ORDER`、`PAYMENT`；`target_role` 为 1（主要对象）或 2（关联对象）。同一案件中的同一资源不能重复关联。`target_id` 是业务资源 ID，不是管理员账号 ID。

### `admin_case_action.status` / `RegulationActionStatus`

`0 CREATED`、`10 REQUESTED`、`20 SUCCEEDED`、`30 FAILED`、`40 CANCELLED`。

- `action_no` 只能代表**管理端对业务服务的一次处置请求**，不得与业务资源的最终状态划等号。
- `REQUESTED`/`SUCCEEDED`/`FAILED` 要求 `requested_at`；`SUCCEEDED`/`FAILED`/`CANCELLED` 要求 `completed_at`。
- 能否执行 `MERCHANT_DISABLE`、`PRODUCT_TAKEDOWN` 等操作，必须由目标业务服务根据权限、资源归属及状态规则执行检查；管理员服务不直接写其他服务数据库。

### `admin_exception_investigation.status` / `InvestigationStatus`

`0 OPEN`、`10 INVESTIGATING`、`20 RESOLVED`、`30 CLOSED`。

- `detection_key` 用于判定**同一个异常事件的重复报告**；相同订单在不同时间再次发生异常，应使用不同事件键。
- 状态 `RESOLVED`/`CLOSED` 要求 `resolved_at`。
- `related_case_id` 可为空；排查发现存在违规时可进一步关联监管案件。
- 这张表代表人工/平台排查过程，**不代表已完成异常补偿**，也不包含真正的补偿任务重试计数。

## 5. 索引、完整性与安全约束

1. 每张表有自增主键；`case_no`、`action_no`、`investigation_no` 等业务号单独唯一。日志主键用于稳定分页。
2. 跨服务资源账号 ID 仅为逻辑引用；只有 admin 自己的表之间存在外键。
3. `admin_case_action (case_target_id, case_id)` 联合外键要求处置目标确实属于同一个监管案件；不会出现动作属于案件 A、对象却来自案件 B。
4. `CHECK` 可约束状态码、优先级、部分时间列条件，但跨表、跨服务校验必须在未来用例中实现。
5. 不在管理数据库复制用户手机号、订单明细、支付敏感字段；日志中的异常原因/请求摘要必须脱敏，不存身份令牌、支付渠道密钥或完整 HTTP 报文。
6. 管理端读取敏感资料时仍需由网关**和业务服务**做鉴权，不能认为只要有管理员身份就有全平台无限数据访问权。
7. 审计日志、状态日志作为追加写记录设计。实际防篡改与保留策略需结合权限分离、备份与运维措施实现，建表本身不代表审计防篡改。

## 6. 迁移文件

1. `V2026110301__create_admin_operation_log.sql`
2. `V2026110302__create_admin_regulation_case.sql`
3. `V2026110303__create_admin_case_target.sql`
4. `V2026110304__create_admin_case_action.sql`
5. `V2026110305__create_admin_case_status_log.sql`
6. `V2026110306__create_admin_exception_investigation.sql`
7. `V2026110307__create_admin_investigation_log.sql`

为避免把未知旧库静默接管，设置 `baseline-on-migrate=false`、`validate-on-migrate=true`、`clean-disabled=true`。**只面向新建空库初始化。** 一旦 Flyway 迁移版本进入生产，后续调整只能新加版本脚本，不能直接修改旧脚本内容。

## 7. 当前不实现

- 不开发后台查询接口、商家审核、商品下架、用户启禁用、管理员权限配置和审计业务写入。
- 不开发 Feign / RabbitMQ / Seata / 定时任务 / 对账 / 补偿流程。
- 不创建 IAM、商家、商品、订单、库存、支付的镜像数据表。
- 所有未定义的处理逻辑使用 `package-info.java` 保留扩展目录；少数状态枚举仅作为数据字典。
