package com.mall.notification.application.notification.result;

import com.mall.notification.domain.notification.model.NotificationMessage;
import java.util.List;

public record NotificationPage(List<NotificationMessage> records, long total, int page, int size) {
}
