package com.mall.iam.infrastructure.generator;

import com.mall.iam.application.account.port.AccountNoGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UuidAccountNoGenerator implements AccountNoGenerator {

    @Override
    public String next() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
