package com.mall.iam.application.account.port.security;

public interface PasswordHasher {
    String hash(String rawPassword);
}
