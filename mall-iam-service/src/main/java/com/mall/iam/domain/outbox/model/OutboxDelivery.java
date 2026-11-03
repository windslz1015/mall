package com.mall.iam.domain.outbox.model;

/** 一条待投递消息的必要字段。 */
public record OutboxDelivery(
        Long id,
        String eventId,
        String eventType,
        String payload,
        int retryCount) { }
