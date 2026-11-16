package com.mall.notification.domain.notification.repository;

import com.mall.notification.domain.notification.model.NotificationMessage;
import java.util.List;

public interface NotificationRepository {
    void saveWelcomeIfAbsent(long accountId, String businessKey,
                             String sourceEventId, String title, String content);

    long countByAccountId(long accountId);

    List<NotificationMessage> findByAccountId(long accountId, int limit, int offset);

    boolean markRead(long accountId, long notificationId);
}
