package com.mall.iam.application.account.port;

public interface PasswordHasher {
    String hash(String rawPassword);
}
