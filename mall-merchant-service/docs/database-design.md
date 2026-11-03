# mall-merchant-service 数据库设计（初始化版）

## 业务边界

商家账号（IAM 中的 `iam_account`）与商家主体（本服务的 `merchant`）是两个不同对象。本服务不保存登录密码、用户名、角色、Token 或用户状态；仅存储跨服务的 `account_id` 数字标识。商品、库存、订单、支付不在本数据库建表。

只有入驻审核通过，才创建 `merchant` 主体。待审核与被拒绝的申请只保留在 `merchant_application`，不会自动变成商家。

## 六张表

| 表 | 作用 | 主键/唯一约束 | 主要索引 |
| --- | --- | --- | --- |
| `merchant_application` | 商家入驻申请与最新审核状态 | `id`，唯一 `application_no` | `(applicant_account_id, submitted_at)`、`(status, submitted_at, id)` |
| `merchant` | 通过审核后的商家经营主体 | `id`，唯一 `merchant_no`，唯一 `source_application_id` | `(status, created_at, id)` |
| `merchant_profile` | 商家展示名、Logo、联系人资料 | `merchant_id`，与主体 1:1 | 主键即可 |
| `merchant_account_binding` | IAM 账号与商家主体的关联关系 | `id`，唯一 `(merchant_id, account_id)` | `(account_id, status, merchant_id)` |
| `merchant_review_log` | 入驻审核日志，追加写 | `id` | `(application_id, reviewed_at, id)` |
| `merchant_status_log` | 商家启用/禁用记录，追加写 | `id` | `(merchant_id, occurred_at, id)` |

## 关系

```text
IAM: iam_account (另一个独立数据库)
             | account_id（逻辑引用，无跨库外键）
             v
  merchant_application 1 ── 0..1 merchant 1 ─── 1 merchant_profile
             |                     |
             N                     N
      merchant_review_log   merchant_account_binding ── account_id -> IAM
                                   |
                                   N
                           merchant_status_log (关联 merchant)
```

`merchant.source_application_id` 具有数据库唯一约束，一个入驻申请至多建立一个商家主体。申请被拒绝后是否允许重新提交，由未来的业务规则定义，不在初始化 DDL 中固化。

`merchant_account_binding` 只预留一个商家关联多个账号的结构，不代表当前已支持多员工权限；当前 IAM 的商家注册流程也尚未实现。绑定账号是否存在、是否有商家角色、是否已被禁用，需未来通过 IAM 验证，数据库无法跨服务约束。

## 状态字段数据字典

- `merchant_application.status`：1 待审核；2 审核通过；3 审核拒绝。
- `merchant.status`：1 正常；2 禁用。
- `merchant_account_binding.status`：1 有效；2 停用。
- `merchant_account_binding.binding_role`：1 负责人；2 运营人员（仅预留）。
- `merchant_review_log.new_status`：2 审核通过；3 审核拒绝。

### 尚未编码的业务规则

- 是否允许同一账号同时申请多家商家；是否允许多个商家共用营业执照号。
- 是否严格只允许一个负责人绑定，以及负责人变更规则。
- 禁用商家后历史订单可查询、但不能创建新交易的检查逻辑。
- 审核幂等、并发审核、事务、注册商家账号与授权同步。

以上规则需要实现应用服务时明确，不应在本次初始化阶段通过猜测直接实现。

## 数据一致性与约束策略

- 关系仅在同库内建立 FK；指向 IAM 的账号 ID **不能**建立跨库外键。
- 所有 `id` 均 `BIGINT UNSIGNED`，与现有 `iam_account.id` 类型一致。
- 实体/审核均有数据库约束、索引和毫秒时间精度；状态启停与审核操作日志单独存储。
- 不使用物理级联删除；历史记录与商家主体不能因为删除关系记录而被意外删除。
- 商家申请的当前审核结果保存在申请表，审核历史保存在日志表，两者在未来代码中应以本地事务一致写入。
- 未来商家主体创建、资料创建、账号关联、审核更新需在同一个商家库本地事务内完成；IAM 侧账号授权属于跨服务协作，不是单库事务。

## Flyway 清单

1. `V2026110301__create_merchant_application.sql`
2. `V2026110302__create_merchant.sql`
3. `V2026110303__create_merchant_profile.sql`
4. `V2026110304__create_merchant_account_binding.sql`
5. `V2026110305__create_merchant_review_log.sql`
6. `V2026110306__create_merchant_status_log.sql`

**执行前必须确保数据库已创建且为空**。Flyway 负责版本管理，不允许手工修改已经成功执行过的 `V` 文件，否则校验将失败。后续新增或变更表，新增更高版本的 migration。

## 非本次交付范围

Controller API、Application Service、领域实体聚合实现、Repository/Mapper/PO、权限校验、RabbitMQ、Seata、任务调度和完整联调均未实现。本次只是可以启动并执行 Flyway 建表的工程初始化版本。
