package com.mall.iam.domain.outbox.model;

public class OutboxEvent {


    private final String eventId;

    private final String aggregateType; //聚合类型

    private final String aggregateId; //聚合标识

    private final OutboxEventType eventType;

    private final String payload;  //序列化的事件内容


    private final String bizKey;// 业务幂等键

    private final OutboxEventStatus status; //投递状态

    private final int retryCount; //重试次数

    private OutboxEvent(
            String eventId,
            String aggregateType,
            String aggregateId,
            OutboxEventType eventType,
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
            OutboxEventType eventType,
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

        if (eventType == null) {
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

    public OutboxEventType getEventType() {
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