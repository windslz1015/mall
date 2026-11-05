package com.mall.iam.domain.outbox.projection;

/** 一条待投递消息的必要字段。本质是数据传递 */
public record OutboxDelivery(
        Long id,
        String eventId,
        String eventType,
        String payload,
        int retryCount) { }
