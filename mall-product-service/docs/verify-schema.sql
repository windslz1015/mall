-- Flyway 成功执行后，确认本服务数据库独立维护的九张业务表。
SELECT table_name
  FROM information_schema.tables
 WHERE table_schema = 'mall_product'
   AND table_type = 'BASE TABLE'
   AND table_name IN (
       'product', 'product_spec', 'product_spec_value', 'product_sku',
       'product_sku_spec_value', 'product_image', 'product_price_change_log',
       'product_status_log', 'product_moderation_log'
   )
 ORDER BY table_name;

-- 应有九条成功执行的 V 版本迁移（另有 Flyway 的 schema_history 表）。
SELECT installed_rank, version, description, success
  FROM mall_product.flyway_schema_history
 ORDER BY installed_rank;

-- 查看商家ID等核心业务索引，以及各表间的外键。
SELECT table_name, constraint_name, referenced_table_name
  FROM information_schema.referential_constraints
 WHERE constraint_schema = 'mall_product'
 ORDER BY table_name, constraint_name;
