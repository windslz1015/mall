package com.mall.iam.domain.outbox.repository;

import com.mall.iam.domain.outbox.model.OutboxDelivery;
import com.mall.iam.domain.outbox.model.OutboxEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository {
    void save(OutboxEvent event);
    List<Long> findDueIds(int limit);
    /** 数据库原子更新，返回 true 的实例才有资格发送该消息。 */
    boolean claim(Long id, String token);
    Optional<OutboxDelivery> findClaimed(Long id, String token);
    void markSent(Long id, String token);
    void markFailed(Long id, String token, LocalDateTime retryAt);
}
