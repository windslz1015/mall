package com.mall.iam.application.account.event;

public record AccountRegisteredEvent(

        String eventId,

        Long accountId,

        String accountNo,

        String username,

        Integer accountType,

        String occurredAt

) {
}