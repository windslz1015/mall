package com.mall.notification.domain.notification.model;

import java.time.LocalDateTime;

/** 站内消息，只有本服务拥有其可写状态。 */
public record NotificationMessage(
        long id,
        long accountId,
        String type,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
    public boolean isRead() {
        return readAt != null;
    }
}
