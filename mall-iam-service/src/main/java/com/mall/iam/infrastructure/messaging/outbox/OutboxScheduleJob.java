package com.mall.iam.infrastructure.messaging.outbox;

import com.mall.iam.domain.outbox.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxScheduleJob {
    private static final Logger log = LoggerFactory.getLogger(OutboxScheduleJob.class);
    private final OutboxEventRepository repository;
    private final OutboxDispatchService dispatcher;
    public OutboxScheduleJob(OutboxEventRepository repository, OutboxDispatchService dispatcher) {
        this.repository = repository;
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelayString = "${iam.outbox.scan-delay-ms:5000}")
    public void scan() {
        try {
            for (Long id : repository.findDueIds(100)) {
                dispatcher.dispatch(id);
            }
        } catch (RuntimeException e) {
            log.error("Outbox 定时补偿扫描失败", e);
        }
    }
}
