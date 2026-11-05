package com.mall.iam.infrastructure.messaging.outbox;

/**
 * IAM 服务的 RabbitMQ 路由定义。
 */
public final class IamRabbitRouting {

    private IamRabbitRouting() {}
    // IAM 服务统一事件交换机
    public static final String EXCHANGE = "iam.events";
    // Account 领域
    public static final String ACCOUNT_REGISTERED = "account.registered";
    public static final String ACCOUNT_PASSWORD_CHANGED = "account.password.changed";
    // Role 领域
    public static final String ROLE_CREATED = "role.created";
    // Permission 领域
    public static final String PERMISSION_GRANTED = "permission.granted";
}