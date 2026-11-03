-- Flyway 迁移成功后执行（仅检查，不修改数据）。
USE mall_inventory;

-- 预期 6 张库存业务表 + Flyway 自动创建的 flyway_schema_history。
SHOW TABLES;

SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;

-- 预期 6 个以 inventory_ 开头的业务表。
SELECT table_name, table_comment
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
      'inventory_stock',
      'inventory_reservation',
      'inventory_operation',
      'inventory_adjustment',
      'inventory_stock_ledger',
      'inventory_reservation_event'
  )
ORDER BY table_name;

-- 主键/唯一约束/索引检查。
SELECT table_name, index_name, non_unique, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name LIKE 'inventory\_%'
GROUP BY table_name, index_name, non_unique
ORDER BY table_name, index_name;

-- 表内外键检查；这里不应出现跨库外键。
SELECT table_name, constraint_name, referenced_table_schema, referenced_table_name
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE()
  AND referenced_table_name IS NOT NULL
ORDER BY table_name, constraint_name;

-- 校验初始数据状态；新库应返回 0、0、0。
SELECT (SELECT COUNT(*) FROM inventory_stock) AS stock_rows,
       (SELECT COUNT(*) FROM inventory_reservation) AS reservation_rows,
       (SELECT COUNT(*) FROM inventory_stock_ledger) AS ledger_rows;
