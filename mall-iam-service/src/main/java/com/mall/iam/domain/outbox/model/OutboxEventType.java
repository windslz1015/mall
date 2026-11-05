package com.mall.iam.domain.outbox.model;

/**
 * Outbox 业务事件类型。
 */
public enum OutboxEventType {


    ACCOUNT_REGISTERED,
    ACCOUNT_PASSWORD_CHANGED,

    // Role 领域
    ROLE_CREATED,

    // Permission 领域
    PERMISSION_GRANTED
}