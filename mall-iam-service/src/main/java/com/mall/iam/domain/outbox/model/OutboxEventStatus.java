package com.mall.iam.domain.outbox.model;

public enum OutboxEventStatus {

    PENDING, //待处理或者待发送

    SENDING, //发送中

    SENT, //已发送

    FAILED //发送失败
}
