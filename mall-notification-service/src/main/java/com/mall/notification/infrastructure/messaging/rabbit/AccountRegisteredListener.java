package com.mall.notification.infrastructure.messaging.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.notification.application.notification.command.AccountRegisteredEvent;
import com.mall.notification.application.notification.service.WelcomeNotificationApplicationService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AccountRegisteredListener {
    private final ObjectMapper objectMapper;
    private final WelcomeNotificationApplicationService welcomeService;

    public AccountRegisteredListener(ObjectMapper objectMapper,
                                     WelcomeNotificationApplicationService welcomeService) {
        this.objectMapper = objectMapper;
        this.welcomeService = welcomeService;
    }

    @RabbitListener(queues = "${notification.rabbit.account-registered-queue:notification.account.registered}")
    public void onRegistered(Message message) {
        if (message.getBody().length == 0 || message.getBody().length > 8192) {
            throw new IllegalArgumentException("注册事件体大小不合法");
        }
        Object typeHeader = message.getMessageProperties().getHeaders().get("eventType");
        if (!"ACCOUNT_REGISTERED".equals(typeHeader)) {
            throw new IllegalArgumentException("注册事件类型不匹配");
        }
        try {
            AccountRegisteredEvent event = objectMapper.readValue(message.getBody(), AccountRegisteredEvent.class);
            String messageId = message.getMessageProperties().getMessageId();
            if (event.eventId() == null || !event.eventId().equals(messageId)) {
                throw new IllegalArgumentException("RabbitMQ消息ID与事件ID不匹配");
            }
            welcomeService.handle(event, message.getBody());
        } catch (IOException ex) {
            throw new IllegalArgumentException("注册事件JSON无法解析", ex);
        }
        // 正常返回才由 Spring AMQP AUTO 模式向 RabbitMQ ACK。
        // 事务失败抛异常，经过有限重试后进入通知服务的死信队列。
    }
}
