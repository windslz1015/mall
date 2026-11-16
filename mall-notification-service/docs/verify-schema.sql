SELECT table_name
  FROM information_schema.tables
 WHERE table_schema = 'mall_notification'
   AND table_type = 'BASE TABLE'
   AND table_name IN ('notification_message', 'inbox_event')
 ORDER BY table_name;

SELECT installed_rank, version, description, success
  FROM mall_notification.flyway_schema_history
 ORDER BY installed_rank;

SHOW INDEX FROM mall_notification.notification_message;
SHOW INDEX FROM mall_notification.inbox_event;

-- 注册用户收到欢迎站内信后的验证SQL（替换为实际账号ID）：
-- SELECT id, account_id, type, title, created_at FROM mall_notification.notification_message WHERE account_id = 123;
