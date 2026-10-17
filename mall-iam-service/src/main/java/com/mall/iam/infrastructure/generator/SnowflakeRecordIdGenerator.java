package com.mall.iam.infrastructure.generator;

import com.mall.iam.application.account.port.identity.RecordIdGenerator;
import org.springframework.stereotype.Component;

@Component
public class SnowflakeRecordIdGenerator
        implements RecordIdGenerator {

    private final SnowflakeAccountNoGenerator generator;

    public SnowflakeRecordIdGenerator(
            SnowflakeAccountNoGenerator generator) {
        this.generator = generator;
    }

    @Override
    public Long nextId() {
        return generator.nextId();
    }
}