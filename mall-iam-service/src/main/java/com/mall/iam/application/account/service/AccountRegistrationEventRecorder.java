package com.mall.iam.application.account.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.iam.application.account.event.AccountRegisteredEvent;
import com.mall.iam.domain.account.model.Account;
import com.mall.iam.domain.outbox.model.OutboxEvent;
import com.mall.iam.domain.outbox.model.OutboxEventType;
import com.mall.iam.domain.outbox.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class AccountRegistrationEventRecorder {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public AccountRegistrationEventRecorder(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {

        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public Long record(Account account) {

        String eventId = UUID.randomUUID().toString();

        String bizKey = "AccountRegistered:" + account.getAccountNo();

        AccountRegisteredEvent registeredEvent =
                new AccountRegisteredEvent(
                        eventId,
                        account.getId(),
                        account.getAccountNo(),
                        account.getUsername(),
                        account.getAccountType().getCode(),
                        Instant.now().toString());
        String payload;

        try {
            payload = objectMapper.writeValueAsString(registeredEvent);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("注册成功事件序列化失败", e);
        }

        OutboxEvent outboxEvent = OutboxEvent.pending(
                eventId,
                "Account",
                account.getAccountNo(),
                OutboxEventType.ACCOUNT_REGISTERED,
                payload,
                bizKey
        );

        return outboxEventRepository.save(outboxEvent);
    }
}