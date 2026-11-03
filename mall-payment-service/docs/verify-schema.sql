-- 1. 核查 8 张业务表（Flyway 自有 flyway_schema_history 不计入业务表）。
SELECT table_name
  FROM information_schema.tables
 WHERE table_schema = 'mall_payment'
   AND table_type = 'BASE TABLE'
   AND table_name IN (
      'payment_order', 'payment_attempt', 'payment_record',
      'payment_notification', 'payment_operation', 'payment_status_log',
      'outbox_event', 'inbox_event'
   )
 ORDER BY table_name;

-- 2. 核查 Flyway 迁移版本（预期 8 个成功的 V2026110301...08）。
SELECT installed_rank, version, description, success
  FROM mall_payment.flyway_schema_history
 ORDER BY installed_rank;

-- 3. 外键只允许在本支付库内部建立。
SELECT table_name, constraint_name, referenced_table_name
  FROM information_schema.referential_constraints
 WHERE constraint_schema = 'mall_payment'
 ORDER BY table_name, constraint_name;

-- 4. 检查金额、状态等 CHECK 约束。
SELECT t.table_name, t.constraint_name, c.check_clause
  FROM information_schema.table_constraints AS t
  JOIN information_schema.check_constraints AS c
    ON t.constraint_schema = c.constraint_schema
   AND t.constraint_name = c.constraint_name
 WHERE t.table_schema = 'mall_payment'
   AND t.constraint_type = 'CHECK'
 ORDER BY t.table_name, t.constraint_name;

-- 5. 检查关键业务唯一约束及回调去重索引。
SHOW INDEX FROM mall_payment.payment_order;
SHOW INDEX FROM mall_payment.payment_attempt;
SHOW INDEX FROM mall_payment.payment_record;
SHOW INDEX FROM mall_payment.payment_notification;
SHOW INDEX FROM mall_payment.payment_operation;
