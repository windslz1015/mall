package com.mall.iam.infrastructure.persistence.outbox.repository;

import com.mall.iam.domain.outbox.model.OutboxDelivery;
import com.mall.iam.domain.outbox.model.OutboxEvent;
import com.mall.iam.domain.outbox.repository.OutboxEventRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class OutboxEventRepositoryImpl implements OutboxEventRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public OutboxEventRepositoryImpl(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public void save(OutboxEvent event) {
        String sql = """
                INSERT INTO outbox_event (id, event_id, aggregate_type, aggregate_id,
                                          event_type, payload, status, retry_count, biz_key)
                VALUES (:id, :eventId, :aggregateType, :aggregateId,
                        :eventType, :payload, 'PENDING', 0, :bizKey)
                """;
        int affected = jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", event.getId()).addValue("eventId", event.getEventId())
                .addValue("aggregateType", event.getAggregateType())
                .addValue("aggregateId", event.getAggregateId())
                .addValue("eventType", event.getEventType()).addValue("payload", event.getPayload())
                .addValue("bizKey", event.getEventType() + ":" + event.getAggregateId()));
        if (affected != 1) throw new IllegalStateException("Outbox事件保存失败");
    }

    @Override public List<Long> findDueIds(int limit) {
        return jdbc.queryForList("""
                SELECT id FROM outbox_event
                WHERE (status IN ('PENDING', 'FAILED')
                        AND (next_retry_at IS NULL OR next_retry_at <= NOW(3)))
                   OR (status = 'SENDING' AND next_retry_at <= NOW(3))
                ORDER BY created_at ASC LIMIT :maxRows
                """, Map.of("maxRows", limit), Long.class);
    }

    @Override public boolean claim(Long id, String token) {
        return jdbc.update("""
                UPDATE outbox_event
                SET status = 'SENDING', claim_token = :token,
                    next_retry_at = DATE_ADD(NOW(3), INTERVAL 60 SECOND)
                WHERE id = :id AND (
                      (status IN ('PENDING','FAILED') AND
                       (next_retry_at IS NULL OR next_retry_at <= NOW(3)))
                   OR (status = 'SENDING' AND next_retry_at <= NOW(3)))
                """, Map.of("id", id, "token", token)) == 1;
    }

    @Override public Optional<OutboxDelivery> findClaimed(Long id, String token) {
        List<OutboxDelivery> events = jdbc.query("""
                SELECT id, event_id, event_type, payload, retry_count
                FROM outbox_event
                WHERE id = :id AND status = 'SENDING' AND claim_token = :token
                """, Map.of("id", id, "token", token), (rs, rowNum) -> new OutboxDelivery(
                rs.getLong("id"), rs.getString("event_id"), rs.getString("event_type"),
                rs.getString("payload"), rs.getInt("retry_count")));
        return events.stream().findFirst();
    }

    @Override public void markSent(Long id, String token) {
        jdbc.update("""
                UPDATE outbox_event
                SET status = 'SENT', claim_token = NULL,
                    next_retry_at = NULL, sent_at = NOW(3)
                WHERE id = :id AND status = 'SENDING' AND claim_token = :token
                """, Map.of("id", id, "token", token));
        // 若租约已被其他节点接管，旧节点不能修改新节点状态。
    }

    @Override public void markFailed(Long id, String token, LocalDateTime retryAt) {
        jdbc.update("""
                UPDATE outbox_event
                SET status = 'FAILED', claim_token = NULL,
                    retry_count = retry_count + 1, next_retry_at = :retryAt
                WHERE id = :id AND status = 'SENDING' AND claim_token = :token
                """, new MapSqlParameterSource().addValue("id", id)
                .addValue("token", token).addValue("retryAt", retryAt));
    }
}
