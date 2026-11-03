-- 在新库 mall_cart 上执行；只读验证，不修改数据。
USE mall_cart;

-- 预期两张业务表 cart、cart_item，以及 flyway_schema_history。
SHOW TABLES;

-- 两条 Flyway 迁移均应 success = 1。
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;

-- 预期 cart、cart_item 均存在；本查询返回 2 行。
SELECT table_name, table_comment
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name IN ('cart', 'cart_item')
ORDER BY table_name;

-- 检查唯一约束与索引。
SELECT table_name, index_name, non_unique,
       GROUP_CONCAT(column_name ORDER BY seq_in_index) AS index_columns
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name IN ('cart', 'cart_item')
GROUP BY table_name, index_name, non_unique
ORDER BY table_name, index_name;

-- 应仅有一条 cart_item -> cart 的本服务内外键。
SELECT table_name, constraint_name,
       referenced_table_schema, referenced_table_name
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE()
  AND referenced_table_name IS NOT NULL
ORDER BY table_name, constraint_name;

-- 检查购物车数量 CHECK 约束（MySQL 8.4 生效）。
SELECT tc.table_name, tc.constraint_name, cc.check_clause
FROM information_schema.table_constraints tc
JOIN information_schema.check_constraints cc
  ON cc.constraint_schema = tc.constraint_schema
 AND cc.constraint_name = tc.constraint_name
WHERE tc.table_schema = DATABASE()
  AND tc.table_name IN ('cart', 'cart_item')
  AND tc.constraint_type = 'CHECK';

-- 新创建且没有业务写入的库，二者应均为 0。
SELECT (SELECT COUNT(*) FROM cart) AS cart_rows,
       (SELECT COUNT(*) FROM cart_item) AS cart_item_rows;
