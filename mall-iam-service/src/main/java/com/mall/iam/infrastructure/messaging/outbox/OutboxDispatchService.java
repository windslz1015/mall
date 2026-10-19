package com.mall.iam.infrastructure.messaging.outbox;

import com.mall.iam.domain.outbox.model.OutboxDelivery;
import com.mall.iam.application.account.port.event.RegistrationEventDispatcher;
import com.mall.iam.domain.outbox.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OutboxDispatchService implements RegistrationEventDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OutboxDispatchService.class);
    private final OutboxEventRepository repository;
    private final OutboxPublisher publisher;
    private final ThreadPoolTaskExecutor executor;

    public OutboxDispatchService(OutboxEventRepository repository, OutboxPublisher publisher,
                                 @Qualifier("outboxExecutor") ThreadPoolTaskExecutor executor) {
        this.repository = repository;
        this.publisher = publisher;
        this.executor = executor;
    }

    /** 此方法只能在数据库事务提交后调用，线程池拒绝也不会丢事件（定时扫描兜底）。 */
    @Override
    public void submit(Long outboxId) {
        try {
            executor.execute(() -> dispatch(outboxId));
        } catch (TaskRejectedException | java.util.concurrent.RejectedExecutionException e) {
            log.warn("Outbox 线程池拥塞，转由定时任务补偿 outboxId={}", outboxId);
        } catch (RuntimeException e) {
            log.warn("Outbox 提交异步任务异常，转由定时任务补偿 outboxId={}", outboxId, e);
        }
    }

    /** 多实例通过 claim 的条件更新抢占消息；宕机由租约过期后重新扫描。 */
    public void dispatch(Long outboxId) {
        String claimToken = UUID.randomUUID().toString();
        try {
            if (!repository.claim(outboxId, claimToken)) return;
        } catch (RuntimeException e) {
            log.error("Outbox 抢占消息失败 outboxId={}", outboxId, e);
            return;
        }
        try {
            OutboxDelivery event = repository.findClaimed(outboxId, claimToken)
                    .orElseThrow(() -> new IllegalStateException("已领取的事件不存在"));
            publisher.publish(event);
            repository.markSent(outboxId, claimToken);
        } catch (Exception e) {
            log.error("Outbox 投递失败 outboxId={}，等待重试", outboxId, e);
            try {
                // 此时无法取得当前 retryCount 也允许安全回退：由 DB 写失败次数；
                // 当前用固定 30s 重试，避免服务异常期间快速重试压垮 MQ。
                repository.markFailed(outboxId, claimToken, LocalDateTime.now().plusSeconds(30));
            } catch (RuntimeException databaseError) {
                log.error("Outbox 更新失败状态失败；60 秒租约到期后重试 id={}", outboxId, databaseError);
            }
        }
    }
}
