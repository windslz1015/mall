-- 1. 核查九张业务表（加上 Flyway 自动维护的 flyway_schema_history 为十张）。
SELECT table_name
  FROM information_schema.tables
 WHERE table_schema = 'mall_order'
   AND table_type = 'BASE TABLE'
   AND table_name IN (
       'trade_order', 'trade_sub_order', 'trade_order_item',
       'trade_order_operation', 'trade_order_cancel_record',
       'trade_order_status_log', 'trade_sub_order_status_log',
       'outbox_event', 'inbox_event'
   )
 ORDER BY table_name;

-- 2. 核查 Flyway 九个 V 版本全部执行成功。
SELECT installed_rank, version, description, success
  FROM mall_order.flyway_schema_history
 ORDER BY installed_rank;

-- 3. 核查本地外键引用；不应出现到其他服务数据库的外键。
SELECT table_name, constraint_name, referenced_table_name
  FROM information_schema.referential_constraints
 WHERE constraint_schema = 'mall_order'
 ORDER BY table_name, constraint_name;

-- 4. 检查关键 CHECK 约束是否创建。
SELECT table_name, constraint_name, check_clause
  FROM information_schema.check_constraints c
  JOIN information_schema.table_constraints t
    ON c.constraint_schema = t.constraint_schema
   AND c.constraint_name = t.constraint_name
 WHERE t.table_schema = 'mall_order'
   AND t.constraint_type = 'CHECK'
 ORDER BY table_name, constraint_name;

-- 5. 检查待支付订单超时扫描索引。
SHOW INDEX FROM mall_order.trade_order;
