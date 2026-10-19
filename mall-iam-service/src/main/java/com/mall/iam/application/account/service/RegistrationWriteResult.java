package com.mall.iam.application.account.service;

import com.mall.iam.application.account.result.RegisterAccountResult;

/** 仅服务内部使用，Outbox ID 不暴露给 HTTP 客户端。 */
public record RegistrationWriteResult(
        RegisterAccountResult account,
        Long outboxId
) { }
