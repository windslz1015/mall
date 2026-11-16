package com.mall.notification.infrastructure.persistence.notification;

import com.mall.notification.domain.notification.model.NotificationMessage;
import com.mall.notification.domain.notification.repository.NotificationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class JdbcNotificationRepository implements NotificationRepository {
    private final JdbcTemplate jdbc;

    public JdbcNotificationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void saveWelcomeIfAbsent(long accountId, String businessKey,
                                    String sourceEventId, String title, String content) {
        // biz_key 的唯一索引可以阻止重复事件ID为同一用户生成多条欢迎通知。
        jdbc.update("""
                INSERT INTO notification_message
                    (account_id, biz_key, source_event_id, type, title, content)
                VALUES (?, ?, ?, 'WELCOME', ?, ?)
                ON DUPLICATE KEY UPDATE id = id
                """, accountId, businessKey, sourceEventId, title, content);
    }

    @Override
    public long countByAccountId(long accountId) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM notification_message WHERE account_id = ?
                """, Long.class, accountId);
        return count == null ? 0 : count;
    }

    @Override
    public List<NotificationMessage> findByAccountId(long accountId, int limit, int offset) {
        return jdbc.query("""
                SELECT id, account_id, type, title, content, created_at, read_at
                  FROM notification_message
                 WHERE account_id = ?
                 ORDER BY created_at DESC, id DESC
                 LIMIT ? OFFSET ?
                """, (rs, rowNum) -> {
            Timestamp readAt = rs.getTimestamp("read_at");
            return new NotificationMessage(
                    rs.getLong("id"), rs.getLong("account_id"),
                    rs.getString("type"), rs.getString("title"), rs.getString("content"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    readAt == null ? null : readAt.toLocalDateTime());
        }, accountId, limit, offset);
    }

    @Override
    public boolean markRead(long accountId, long notificationId) {
        jdbc.update("""
                UPDATE notification_message
                   SET read_at = CURRENT_TIMESTAMP(3)
                 WHERE id = ? AND account_id = ? AND read_at IS NULL
                """, notificationId, accountId);
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM notification_message WHERE id = ? AND account_id = ?
                """, Integer.class, notificationId, accountId);
        return count != null && count > 0;
    }
}
