-- 1. 应存在 7 张业务表；flyway_schema_history 属于 Flyway 自身管理元数据。
SELECT table_name
  FROM information_schema.tables
 WHERE table_schema = 'mall_admin' AND table_type = 'BASE TABLE'
   AND table_name IN (
       'admin_operation_log', 'admin_regulation_case', 'admin_case_target',
       'admin_case_action', 'admin_case_status_log',
       'admin_exception_investigation', 'admin_investigation_log'
   )
 ORDER BY table_name;

-- 2. 7 条迁移版本应都为 success=1。
SELECT installed_rank, version, description, success
  FROM mall_admin.flyway_schema_history
 ORDER BY installed_rank;

-- 3. 仅应引用 admin 本地表（不存在跨库外键）。
SELECT table_name, constraint_name, referenced_table_name
  FROM information_schema.referential_constraints
 WHERE constraint_schema = 'mall_admin'
 ORDER BY table_name, constraint_name;

-- 4. 检查约束、关键检索/幂等索引。
SELECT t.table_name, t.constraint_name, c.check_clause
  FROM information_schema.table_constraints AS t
  JOIN information_schema.check_constraints AS c
    ON t.constraint_schema = c.constraint_schema
   AND t.constraint_name = c.constraint_name
 WHERE t.table_schema = 'mall_admin' AND t.constraint_type = 'CHECK'
 ORDER BY t.table_name, t.constraint_name;

SHOW INDEX FROM mall_admin.admin_operation_log;
SHOW INDEX FROM mall_admin.admin_case_target;
SHOW INDEX FROM mall_admin.admin_case_action;
SHOW INDEX FROM mall_admin.admin_exception_investigation;
