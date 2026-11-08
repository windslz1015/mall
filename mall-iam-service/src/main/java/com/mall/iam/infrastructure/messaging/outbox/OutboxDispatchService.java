package com.mall.iam.infrastructure.messaging.outbox;

import com.mall.iam.application.shared.port.outbox.OutboxDispatchPort;
import com.mall.iam.domain.outbox.projection.OutboxDelivery;
import com.mall.iam.domain.outbox.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

@Service
public class OutboxDispatchService implements OutboxDispatchPort {
    private static final Logger log = LoggerFactory.getLogger(OutboxDispatchService.class);
    private final OutboxEventRepository repository;
    private final OutboxPublisher publisher;
    private final ThreadPoolTaskExecutor executor;
    /**
     * 当前 JVM 中已经提交或正在执行的 Outbox 事件 ID。
     *
     * 防止同一个事件在消息尚未开始处理时，
     * 被连续扫描并重复放入线程池。
     */
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();

    public OutboxDispatchService(OutboxEventRepository repository, OutboxPublisher publisher,
                                 @Qualifier("outboxExecutor") ThreadPoolTaskExecutor executor) {
        this.repository = repository;
        this.publisher = publisher;
        this.executor = executor;
    }

    /**此方法只能在数据库事务提交后调用，线程池拒绝也不会丢事件（定时扫描兜底）。 */
    @Override
    public void submit(Long outboxId) {
        submitIfAbsent(outboxId);
    }
    /**
     * 将 Outbox 事件提交到线程池。
     *
     * @return true：成功提交，或者已经在当前 JVM 排队/执行
     *         false：线程池拒绝任务
     */
    public boolean submitIfAbsent(Long outboxId) {
        // 1本地去重
        if (!inFlight.add(outboxId)) {
            log.debug("Outbox 事件已经提交或正在执行 outboxId={}", outboxId);
            return true;
        }
        try {
            // 2提交线程池
            executor.execute(() -> {
                try {
                    dispatch(outboxId);} // 3执行实际消息投递
                finally {
                    inFlight.remove(outboxId);// 4任务结束后释放本机去重标记
                }});
            return true;
        } catch (RejectedExecutionException e) {
            inFlight.remove(outboxId);//5线程池拒绝，释放标记，等待下一轮扫描
            log.warn("Outbox 线程池拒绝任务，等待下轮定时扫描 outboxId={}", outboxId);
            return false;
        } catch (RuntimeException e) {
            inFlight.remove(outboxId);
            log.error("Outbox 异步任务提交异常 outboxId={}", outboxId, e);
            return false;
        }
    }

    /**多实例通过claim的条件更新抢占消息；宕机由租约过期后重新扫描。*/
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
