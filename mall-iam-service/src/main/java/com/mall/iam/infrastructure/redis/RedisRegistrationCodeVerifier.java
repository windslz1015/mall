package com.mall.iam.infrastructure.redis;

import com.mall.common.exception.BusinessException;
import com.mall.iam.application.account.port.verification.RegistrationCodeVerifier;
import com.mall.iam.domain.account.error.AccountErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 使用 iam:register:{phone}:code 键，三个关联键拥有相同的 Redis Cluster hash tag。
 * 发送验证码接口尚未在提交的项目中实现，写入方必须统一使用此键约定。
 * reserve 不删除验证码。通过 token + TTL 实现并发互斥；MySQL 提交以后再 confirm 删除。
 */
@Component
public class RedisRegistrationCodeVerifier implements RegistrationCodeVerifier {
    private static final String PREFIX = "iam:register:";
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_SECONDS = 600;
    private final StringRedisTemplate redis;
    private final int leaseSeconds;
    private final DefaultRedisScript<Long> reserveScript;
    private final DefaultRedisScript<Long> confirmScript;
    private final DefaultRedisScript<Long> releaseScript;

    public RedisRegistrationCodeVerifier(StringRedisTemplate redis,
                                         @Value("${iam.registration.code-lease-seconds:120}") int leaseSeconds) {
        this.redis = redis;
        this.leaseSeconds = leaseSeconds;
        if (leaseSeconds < 30) throw new IllegalArgumentException("验证码租约不能小于30秒");
        reserveScript = script("""
                if redis.call('EXISTS', KEYS[3]) == 1 then return -3 end
                local attempts = tonumber(redis.call('GET', KEYS[2]) or '0')
                if attempts >= tonumber(ARGV[3]) then return -2 end
                local savedCode = redis.call('GET', KEYS[1])
                if not savedCode then return -1 end
                if savedCode ~= ARGV[1] then
                  local nextCount = redis.call('INCR', KEYS[2])
                  if nextCount == 1 then redis.call('EXPIRE', KEYS[2], tonumber(ARGV[4])) end
                  if nextCount >= tonumber(ARGV[3]) then return -2 end
                  return 0
                end
                redis.call('SET', KEYS[3], ARGV[2], 'EX', tonumber(ARGV[5]), 'NX')
                return 1
                """);
        confirmScript = script("""
                if redis.call('GET', KEYS[3]) ~= ARGV[1] then return 0 end
                if redis.call('GET', KEYS[1]) == ARGV[2] then
                  redis.call('DEL', KEYS[1], KEYS[2], KEYS[3])
                else
                  redis.call('DEL', KEYS[3])
                end
                return 1
                """);
        releaseScript = script("""
                if redis.call('GET', KEYS[3]) ~= ARGV[1] then return 0 end
                redis.call('DEL', KEYS[3])
                return 1
                """);
    }

    @Override
    public String reserve(String phone, String code) {
        String token = UUID.randomUUID().toString();
        Long result = redis.execute(reserveScript, keys(phone), code, token,
                String.valueOf(MAX_ATTEMPTS), String.valueOf(LOCK_SECONDS), String.valueOf(leaseSeconds));
        if (result == null) throw new IllegalStateException("Redis 验证码校验失败");
        return switch (result.intValue()) {
            case 1 -> token;
            case -1 -> throw new BusinessException(AccountErrorCode.VERIFICATION_CODE_EXPIRED);
            case 0 -> throw new BusinessException(AccountErrorCode.VERIFICATION_CODE_INVALID);
            case -2 -> throw new BusinessException(AccountErrorCode.VERIFICATION_CODE_LOCKED);
            case -3 -> throw new BusinessException(AccountErrorCode.VERIFICATION_CODE_BUSY);
            default -> throw new IllegalStateException("Redis 未知校验结果: " + result);
        };
    }

    @Override public void confirm(String phone, String token, String code) {
        Long value = redis.execute(confirmScript, keys(phone), token, code);
        if (value == null || value != 1L) throw new IllegalStateException("验证码租约已不存在或已失效");
    }

    @Override public void release(String phone, String token) {
        Long value = redis.execute(releaseScript, keys(phone), token);
        if (value == null) throw new IllegalStateException("Redis 释放验证码租约失败");
    }

    private static List<String> keys(String phone) {
        String phoneTag = "{" + phone + "}";
        return List.of(PREFIX + phoneTag + ":code", PREFIX + phoneTag + ":attempts", PREFIX + phoneTag + ":lease");
    }
    private static DefaultRedisScript<Long> script(String lua) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(lua);
        script.setResultType(Long.class);
        return script;
    }
}
