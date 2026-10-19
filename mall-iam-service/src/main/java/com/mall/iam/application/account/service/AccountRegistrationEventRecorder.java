package com.mall.iam.application.account.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.mall.iam.application.account.event.AccountRegisteredEvent;
import com.mall.iam.application.account.port.identity.RecordIdGenerator;

import com.mall.iam.domain.account.model.Account;
import com.mall.iam.domain.outbox.model.OutboxEvent;
import com.mall.iam.domain.outbox.repository.OutboxEventRepository;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class AccountRegistrationEventRecorder {

    private final OutboxEventRepository outboxEventRepository;

    private final RecordIdGenerator recordIdGenerator;

    private final ObjectMapper objectMapper;

    public AccountRegistrationEventRecorder(
            OutboxEventRepository outboxEventRepository,
            RecordIdGenerator recordIdGenerator,
            ObjectMapper objectMapper) {

        this.outboxEventRepository = outboxEventRepository;
        this.recordIdGenerator = recordIdGenerator;
        this.objectMapper = objectMapper;
    }

    public Long record(Account account) {

        // 1. 生成全局唯一事件ID
        String eventId = UUID.randomUUID().toString();

        // 2. 构建注册成功事件
        AccountRegisteredEvent registeredEvent =
                new AccountRegisteredEvent(
                        eventId,
                        account.getId(),
                        account.getAccountNo(),
                        account.getUsername(),
                        account.getAccountType().getCode(),
                        Instant.now().toString()
                );

        // 3. 序列化事件
        String payload;

        try {

            payload = objectMapper.writeValueAsString(
                    registeredEvent
            );

        } catch (JsonProcessingException e) {

            throw new IllegalStateException(
                    "注册成功事件序列化失败",
                    e
            );
        }

        // 4. 创建待发送Outbox记录
        OutboxEvent outboxEvent = OutboxEvent.pending(
                recordIdGenerator.nextId(),
                eventId,
                "Account",
                account.getAccountNo(),
                "AccountRegistered",
                payload
        );

        // 5. 保存到MySQL
        outboxEventRepository.save(outboxEvent);
        return outboxEvent.getId();
    }
}
