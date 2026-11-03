-- 使用具有 information_schema 和 mall_reliability 查询权限的账号运行。
-- 1. 应存在 8 张业务表；Flyway 自动创建的 flyway_schema_history 不计入。
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'mall_reliability'
  AND table_type = 'BASE TABLE'
  AND table_name IN (
    'reliability_job_definition',
    'reliability_job_execution',
    'reliability_recovery_task',
    'reliability_recovery_attempt',
    'reliability_message_failure',
    'reliability_reconciliation_batch',
    'reliability_reconciliation_difference',
    'reliability_reconciliation_resolution_log'
  )
ORDER BY table_name;

-- 2. Flyway 迁移应有 8 个成功版本。
SELECT installed_rank, version, description, success
FROM mall_reliability.flyway_schema_history
ORDER BY installed_rank;

-- 3. 本库外键必须仅指向 mall_reliability 中本服务表。
SELECT constraint_name, table_name, referenced_table_name
FROM information_schema.referential_constraints
WHERE constraint_schema = 'mall_reliability'
ORDER BY table_name, constraint_name;

-- 4. 数据库约束与关键幂等/重试索引。
SELECT t.table_name, t.constraint_name, c.check_clause
FROM information_schema.table_constraints AS t
JOIN information_schema.check_constraints AS c
  ON t.constraint_schema = c.constraint_schema
 AND t.constraint_name = c.constraint_name
WHERE t.table_schema = 'mall_reliability' AND t.constraint_type = 'CHECK'
ORDER BY t.table_name, t.constraint_name;

SHOW INDEX FROM mall_reliability.reliability_job_execution;
SHOW INDEX FROM mall_reliability.reliability_recovery_task;
SHOW INDEX FROM mall_reliability.reliability_message_failure;
SHOW INDEX FROM mall_reliability.reliability_reconciliation_batch;
SHOW INDEX FROM mall_reliability.reliability_reconciliation_difference;
