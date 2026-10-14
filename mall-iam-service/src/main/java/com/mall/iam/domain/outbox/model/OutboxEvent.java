package com.mall.iam.domain.outbox.model;

public class OutboxEvent {

    private final Long id;

    private final String eventId;

    private final String aggregateType;

    private final String aggregateId;

    private final String eventType;

    private final String payload;

    private final OutboxEventStatus status;

    private final int retryCount;

    private OutboxEvent(
            Long id,
            String eventId,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload,
            OutboxEventStatus status,
            int retryCount) {

        this.id = id;
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
        this.retryCount = retryCount;
    }

    // 创建待发送事件
    public static OutboxEvent pending(
            Long id,
            String eventId,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("事件主键不能为空");
        }

        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("事件标识不能为空");
        }

        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException("聚合标识不能为空");
        }

        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("事件内容不能为空");
        }

        return new OutboxEvent(
                id,
                eventId,
                aggregateType,
                aggregateId,
                eventType,
                payload,
                OutboxEventStatus.PENDING,
                0
        );
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxEventStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }
}