package com.mall.iam.application.shared.port.outbox;

/**
 * Outbox 即时投递触发端口。
 *
 * 业务事务提交后调用。
 * 异步失败由定时扫描补偿。
 */
public interface OutboxDispatchPort {

    void submit(Long outboxId);
}