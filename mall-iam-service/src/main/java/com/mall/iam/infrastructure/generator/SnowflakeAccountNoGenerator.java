package com.mall.iam.infrastructure.generator;

import com.mall.iam.application.account.port.identity.AccountNoGenerator;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class SnowflakeAccountNoGenerator
        implements AccountNoGenerator {

    private final SnowflakeIdGenerator generator;

    public SnowflakeAccountNoGenerator(
            SnowflakeIdGenerator generator) {
        this.generator = generator;
    }

    @Override
    public String next() {
        return "AC" + generator.nextId();
    }
}