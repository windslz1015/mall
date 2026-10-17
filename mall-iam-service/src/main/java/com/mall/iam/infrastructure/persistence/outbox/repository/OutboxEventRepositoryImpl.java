package com.mall.iam.infrastructure.persistence.outbox.repository;

import com.mall.iam.domain.outbox.model.OutboxEvent;
import com.mall.iam.domain.outbox.repository.OutboxEventRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxEventRepositoryImpl
        implements OutboxEventRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OutboxEventRepositoryImpl(
            NamedParameterJdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(OutboxEvent event) {

        String sql = """
                INSERT INTO outbox_event (
                    id,
                    event_id,
                    aggregate_type,
                    aggregate_id,
                    event_type,
                    payload,
                    status,
                    retry_count,
                    biz_key
                )
                VALUES (
                    :id,
                    :eventId,
                    :aggregateType,
                    :aggregateId,
                    :eventType,
                    :payload,
                    :status,
                    :retryCount,
                    :bizKey
                )
                """;

        String bizKey =
                event.getEventType() + ":" + event.getAggregateId();

        var params = new MapSqlParameterSource()
                .addValue("id", event.getId())
                .addValue("eventId", event.getEventId())
                .addValue("aggregateType", event.getAggregateType())
                .addValue("aggregateId", event.getAggregateId())
                .addValue("eventType", event.getEventType())
                .addValue("payload", event.getPayload())
                .addValue("status", event.getStatus().name())
                .addValue("retryCount", event.getRetryCount())
                .addValue("bizKey", bizKey);

        int rows = jdbcTemplate.update(sql, params);

        if (rows != 1) {
            throw new IllegalStateException(
                    "Outbox事件保存失败"
            );
        }
    }
}