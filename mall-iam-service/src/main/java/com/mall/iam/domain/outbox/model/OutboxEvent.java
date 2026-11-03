package com.mall.iam.domain.outbox.model;

public class OutboxEvent {


    private final String eventId;

    private final String aggregateType;

    private final String aggregateId;

    private final String eventType;

    private final String payload;

    // 业务幂等键
    private final String bizKey;

    private final OutboxEventStatus status;

    private final int retryCount;

    private OutboxEvent(
            String eventId,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload,
            String bizKey,
            OutboxEventStatus status,
            int retryCount) {


        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.bizKey = bizKey;
        this.status = status;
        this.retryCount = retryCount;
    }

    // 创建待发送事件
    public static OutboxEvent pending(
            String eventId,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload,
            String bizKey) {


        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("事件标识不能为空");
        }

        if (aggregateType == null || aggregateType.isBlank()) {
            throw new IllegalArgumentException("聚合类型不能为空");
        }

        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException("聚合标识不能为空");
        }

        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("事件类型不能为空");
        }

        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("事件内容不能为空");
        }

        if (bizKey == null || bizKey.isBlank()) {
            throw new IllegalArgumentException("业务幂等键不能为空");
        }

        return new OutboxEvent(
                eventId,
                aggregateType,
                aggregateId,
                eventType,
                payload,
                bizKey,
                OutboxEventStatus.PENDING,
                0
        );
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

    public String getBizKey() {
        return bizKey;
    }

    public OutboxEventStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }
}