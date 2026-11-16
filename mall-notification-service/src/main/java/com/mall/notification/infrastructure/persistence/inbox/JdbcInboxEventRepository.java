package com.mall.notification.infrastructure.persistence.inbox;

import com.mall.notification.domain.inbox.repository.InboxEventRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcInboxEventRepository implements InboxEventRepository {
    private final JdbcTemplate jdbc;

    public JdbcInboxEventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean markProcessedIfAbsent(String consumerName, String eventId, String payloadSha256) {
        int created = jdbc.update("""
                INSERT IGNORE INTO inbox_event
                    (consumer_name, event_id, source_service, event_type, payload_sha256, status, processed_at)
                VALUES (?, ?, 'iam-service', 'ACCOUNT_REGISTERED', ?, 'PROCESSED', CURRENT_TIMESTAMP(3))
                """, consumerName, eventId, payloadSha256);
        if (created == 1) {
            return true;
        }
        String previousHash = jdbc.queryForObject("""
                SELECT payload_sha256 FROM inbox_event
                 WHERE consumer_name = ? AND event_id = ?
                """, String.class, consumerName, eventId);
        if (!payloadSha256.equals(previousHash)) {
            throw new IllegalStateException("相同eventId对应不同事件负载，拒绝处理");
        }
        return false;
    }
}
