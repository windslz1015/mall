# 商品微服务数据库设计（仅初始化）

## 业务边界

- `mall_product` 是 `product-service` 唯一拥有的业务数据库，**只负责商品/SPU、SKU、规格、图片引用、当前报价、上下架和平台商品监管记录**。
- `product.merchant_id` 指向商家服务的 `merchant.id`，是**逻辑引用**，不创建跨服务外键；不能通过 product 表代替 merchant 主体。
- 商品创建与上架时，需要未来调用 `merchant-service` 确认商家状态；此能力暂不实现。
- `inventory-service` 独立拥有库存事实，**本库不建库存表、不保存可用库存/锁定库存**。
- `order-service` 负责订单项商品/规格/价格快照，商品更新**不能影响历史订单**。
- 管理员账号由 IAM 服务拥有，监管记录中的 `admin_account_id` 不设置跨库 FK。

## 九张业务表

| 表名 | 描述 | 关键约束 |
| --- | --- | --- |
| `product` | 商品 SPU 基础信息与当前上下架状态 | `product_no` 全局唯一；商家+状态联合索引 |
| `product_spec` | SPU 规格项，如颜色、容量 | `(product_id, spec_name)` 唯一；商品内部外键 |
| `product_spec_value` | 规格项可选值，如黑色、128G | `(spec_id, value_name)` 唯一 |
| `product_sku` | 可定价、可交易的 SKU | `sku_no` 唯一；`(product_id, spec_signature)` 唯一；价格非负 |
| `product_sku_spec_value` | SKU 选择了哪些规格值 | 每个 SKU 的每个规格只选一个值；复合 FK 防止串商品 |
| `product_image` | 商品封面/图集和 SKU 图片的 OSS Key | 每个商品至多 1 个封面；SKU 图片必须归属于同一商品 |
| `product_price_change_log` | SKU 价格变更记录 | 旧新价格非负、不同；SKU 与商品绑定一致 |
| `product_status_log` | 上架、下架、强制下架等状态变更历史 | 前后状态不同；操作人类型和身份一致 |
| `product_moderation_log` | 平台强制下架专项监管记录 | 关联同商品的状态变更；一条状态变更只有一条监管记录 |

Flyway 自己还会建立 `flyway_schema_history`，它不属于业务表。

## 领域关系与边界

```text
merchant-service
    merchant.id --(逻辑关联 merchant_id)--> product [SPU]
                                               | 1:N product_sku [SKU]
                                               | 1:N product_spec [规格项]
                                               |        | 1:N product_spec_value [规格值]
                                               | 1:N product_image [图片引用]
                                               | 1:N product_status_log
                                               | 1:N product_moderation_log
                                               |
                                product_sku <--+--> product_sku_spec_value --> product_spec_value
                                      |
                                      +--> product_price_change_log
                                      |
                                      +---- SKU ID --> inventory-service（逻辑引用，不是库存表）
```

### SKU 规格组合约束说明

1. `product_sku_spec_value` 的 `product_id` 不是独立业务属性，而是为了**组合外键**：保证 SKU、规格项、规格值关联到同一个 SPU。
2. `product_sku.spec_signature`：由未来的业务实现将规格项与选择值按 `spec_id` 排序，序列化为稳定文本并计算 SHA-256 小写十六进制串，目的是防止同一个 SPU 重复创建同规格 SKU。
3. **没有规格的商品**仍创建一个默认 SKU；该 SKU 的签名是对空规格组合计算的指纹。
4. 数据库只保证引用/唯一性，不会自动验证 SHA-256 是否与关联表内容相符；应用层创建或修改 SKU 时必须在本地事务中维护两者一致，必要时增加集成测试。
5. **同一个 SKU 规格集合的完整性**（例如是否包含该 SPU 所有必选规格）属于业务规则，初始化 DDL 不强行推测。

## 数据字典

- `product.status`：`0=DRAFT` 草稿，`1=ON_SALE` 已上架，`2=OFF_SALE` 商家下架，`3=FORCED_OFF_SALE` 平台强制下架。
- `product_sku.status`：`1=ACTIVE` 可选，`2=INACTIVE` 停用；商品、商家状态和库存仍需另行判断。
- `product_image.image_type`：`1=COVER` 封面，`2=GALLERY` 图集/详情图，`3=SKU` SKU 图片。
- `product_status_log.operator_type`：`1=MERCHANT` 商家，`2=ADMIN` 平台管理员，`3=SYSTEM` 系统。
- `product_moderation_log.action_type`：`1=FORCE_OFF_SALE` 平台强制下架（当前仅实现数据模型）。
- SKU `sale_price` 使用 `DECIMAL(18,2)`；币种默认 `CNY`。**本期只计划人民币交易**，如需多币种交易应先确认业务规则；没有在表中设计换汇、跨币种结算。
- `created_at/updated_at/occurred_at/changed_at` 使用 `DATETIME(3)`；所有 ID 类型为 `BIGINT UNSIGNED`；各可更新主表提供乐观锁 `version`。

## 实体一致性要求（未来实现时遵守）

- 上/下架：更新 `product.status` 与追加 `product_status_log` 必须在同一商品库事务中完成；平台强制下架时还需追加 `product_moderation_log`。
- 改价：更新 `product_sku.sale_price` 与追加 `product_price_change_log` 在同一事务中完成；订单项使用独立成交快照。
- 跨服务：商品上架前商家必须审核通过且状态正常；商品已下架/强制下架不可参与新交易。这些校验不能只依赖网关权限控制。
- 当前 `merchant_id`、操作人 `account_id` 均为逻辑引用，数据库不应尝试跨库级联删除。
- **这里没有创建任何仓储实现、Controller、应用服务或 SKU/商品 CRUD 功能；只有状态枚举和包结构占位。**

## Flyway 顺序（9 个独立文件）

1. `V2026110301__create_product.sql`
2. `V2026110302__create_product_spec.sql`
3. `V2026110303__create_product_spec_value.sql`
4. `V2026110304__create_product_sku.sql`
5. `V2026110305__create_product_sku_spec_value.sql`
6. `V2026110306__create_product_image.sql`
7. `V2026110307__create_product_price_change_log.sql`
8. `V2026110308__create_product_status_log.sql`
9. `V2026110309__create_product_moderation_log.sql`

若已在某个库执行过旧版产品服务 migration，**不能直接把旧文件覆盖为当前版本**；应先基于现有 Flyway history 规划新增迁移。当前安装包面向全新空的 `mall_product` 数据库。

## 暂不初始化的内容

- 商品分类、搜索引擎全文索引、标签、品牌、促销、优惠券、评论及多币种；这些业务需求未明确。
- 库存数值、订单快照、购物车项、支付单、商家账号数据。
- 业务接口、远程客户端、Repository/Mapper/PO 实现、RabbitMQ、Seata、OSS 上传逻辑。
