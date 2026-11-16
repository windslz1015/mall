package com.mall.notification.application.notification.service;

import com.mall.notification.application.notification.command.AccountRegisteredEvent;
import com.mall.notification.domain.inbox.repository.InboxEventRepository;
import com.mall.notification.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WelcomeNotificationApplicationServiceTest {
    private InboxEventRepository inbox;
    private NotificationRepository notification;
    private WelcomeNotificationApplicationService service;

    private final AccountRegisteredEvent event = new AccountRegisteredEvent(
            "6c95d48a-4b9a-4c0a-84f2-e137db4b5c63", 42L,
            "account0001", "测试用户", 1, "2026-11-16T10:00:00Z");
    private final byte[] body = "{\"eventId\":\"sample\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    @BeforeEach
    void setup() {
        inbox = mock(InboxEventRepository.class);
        notification = mock(NotificationRepository.class);
        service = new WelcomeNotificationApplicationService(inbox, notification);
    }

    @Test
    void createsWelcomeForFirstNormalUserEvent() {
        when(inbox.markProcessedIfAbsent(eq("notification.welcome"), eq(event.eventId()), anyString()))
                .thenReturn(true);
        service.handle(event, body);
        verify(notification).saveWelcomeIfAbsent(eq(42L), eq("WELCOME_REGISTERED:account0001"),
                eq(event.eventId()), eq("欢迎加入商城"), contains("测试用户"));
    }

    @Test
    void doesNotDuplicateWelcomeOnEventRedelivery() {
        when(inbox.markProcessedIfAbsent(eq("notification.welcome"), eq(event.eventId()), anyString()))
                .thenReturn(false);
        service.handle(event, body);
        verifyNoInteractions(notification);
    }

    @Test
    void skipsMerchantRegistrationEvents() {
        AccountRegisteredEvent merchant = new AccountRegisteredEvent(event.eventId(), 42L,
                "account0001", "测试商家", 2, "2026-11-16T10:00:00Z");
        service.handle(merchant, body);
        verifyNoInteractions(inbox, notification);
    }
}
