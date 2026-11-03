package com.mall.iam.application.account.port.event;

/** 在 MySQL 事务已经提交之后触发 Outbox 即时投递；失败由扫描任务补偿。 */
public interface RegistrationEventDispatcher {
    void submit(Long outboxId);
}
