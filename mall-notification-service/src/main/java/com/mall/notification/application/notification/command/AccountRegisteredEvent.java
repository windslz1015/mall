package com.mall.notification.application.notification.command;

/** 与 IAM 的 AccountRegisteredEvent JSON 字段逐一对应，避免跨服务编译依赖。 */
public record AccountRegisteredEvent(
        String eventId,
        Long accountId,
        String accountNo,
        String username,
        Integer accountType,
        String occurredAt
) {
}
