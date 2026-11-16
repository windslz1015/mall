package com.mall.notification.application.notification.service;

import com.mall.notification.application.notification.command.AccountRegisteredEvent;
import com.mall.notification.domain.inbox.repository.InboxEventRepository;
import com.mall.notification.domain.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class WelcomeNotificationApplicationService {
    private static final String CONSUMER_NAME = "notification.welcome";
    private final InboxEventRepository inbox;
    private final NotificationRepository notifications;

    public WelcomeNotificationApplicationService(InboxEventRepository inbox,
                                                  NotificationRepository notifications) {
        this.inbox = inbox;
        this.notifications = notifications;
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(AccountRegisteredEvent event, byte[] originalBody) {
        validate(event, originalBody);

        // 只为普通用户生成欢迎通知；IAM AccountType.NORMAL_USER = 1。
        if (event.accountType() != 1) {
            return;
        }

        String digest = sha256(originalBody);
        if (!inbox.markProcessedIfAbsent(CONSUMER_NAME, event.eventId(), digest)) {
            return;
        }

        String username = event.username().strip();
        String title = "欢迎加入商城";
        String content = "欢迎你，" + username + "！账号已注册成功，祝你购物愉快。";
        String businessKey = "WELCOME_REGISTERED:" + event.accountNo();
        notifications.saveWelcomeIfAbsent(event.accountId(), businessKey,
                event.eventId(), title, content);
    }

    private static void validate(AccountRegisteredEvent event, byte[] body) {
        if (event == null || body == null || body.length == 0 || body.length > 8192) {
            throw new IllegalArgumentException("注册事件为空或长度超限");
        }
        if (event.eventId() == null || !event.eventId().matches(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw new IllegalArgumentException("注册事件ID无效");
        }
        // 检查事件ID满足UUID格式，账号编号不允许控制字符。
        UUID.fromString(event.eventId());
        if (event.accountId() == null || event.accountId() <= 0
                || event.accountNo() == null || !event.accountNo().matches("[A-Za-z0-9_-]{1,64}")
                || event.accountType() == null
                || event.username() == null || event.username().isBlank()
                || event.username().length() > 64
                || event.username().chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("注册事件字段不合法");
        }
        try {
            Instant.parse(event.occurredAt());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("注册事件发生时间不合法", ex);
        }
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256不可用", ex);
        }
    }
}
