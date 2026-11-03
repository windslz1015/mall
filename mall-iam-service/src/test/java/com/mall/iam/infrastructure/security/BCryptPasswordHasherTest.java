
package com.mall.iam.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


/*
BCryptPasswordEncoder：Spring Security 提供的密码加密工具。
BCryptPasswordHasher：你自己写的基础设施层实现，用来封装 BCrypt 密码加密功能。
 */

class BCryptPasswordHasherTest {

    @Test
    void shouldHashAndVerifyPassword() {

        BCryptPasswordHasher hasher =
                new BCryptPasswordHasher();

        String rawPassword = "Test@123456";

        String hash = hasher.hash(rawPassword);

        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        assertTrue(encoder.matches(rawPassword, hash));
        assertNotEquals(rawPassword, hash);
    }
}