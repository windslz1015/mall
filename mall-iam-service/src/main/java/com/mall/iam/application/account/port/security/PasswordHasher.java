package com.mall.iam.application.account.port.security;

public interface PasswordHasher {
    String hash(String rawPassword);

    /** 与数据库存储的 BCrypt 哈希比较，不做解密。 */
    boolean matches(String rawPassword, String passwordHash);

}
