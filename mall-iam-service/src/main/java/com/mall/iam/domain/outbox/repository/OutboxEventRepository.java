package com.mall.iam.domain.outbox.repository;

import com.mall.iam.domain.outbox.model.OutboxEvent;

public interface OutboxEventRepository {

    void save(OutboxEvent event);
}