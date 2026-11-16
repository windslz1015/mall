package com.mall.notification.application.notification.service;

import com.mall.notification.application.notification.result.NotificationPage;
import com.mall.notification.domain.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationQueryApplicationService {
    private final NotificationRepository notifications;

    public NotificationQueryApplicationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public NotificationPage list(long accountId, int page, int size) {
        int offset = (page - 1) * size;
        return new NotificationPage(notifications.findByAccountId(accountId, size, offset),
                notifications.countByAccountId(accountId), page, size);
    }

    @Transactional
    public boolean markRead(long accountId, long notificationId) {
        return notifications.markRead(accountId, notificationId);
    }
}
