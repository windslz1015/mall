
package com.mall.iam.infrastructure.redis;

import com.mall.common.exception.BusinessException;
import com.mall.iam.application.account.port.verification.RegistrationCodeVerifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 脚本返回值说明：
 * <ul>
 *   <li> 1 : 验证成功，验证码和错误计数已被删除</li>
 *   <li> 0 : 验证码错误，但未达到最大尝试次数</li>
 *   <li>-1 : 验证码不存在或已过期</li>
 *   <li>-2 : 已达到最大错误次数，当前被锁定</li>
 * </ul>
 */

@Component
public class RedisRegistrationCodeVerifier
        implements RegistrationCodeVerifier {

    private static final String CODE_KEY_PREFIX =
            "iam:register:code:";

    private static final String ATTEMPTS_KEY_PREFIX =
            "iam:register:attempts:";

    // 最大错误次数
    private static final int MAX_ATTEMPTS = 5;

    // 错误限制时长（秒）
    private static final int LOCK_SECONDS = 600;

    private final StringRedisTemplate redisTemplate;

    private final DefaultRedisScript<Long> verifyScript;

    public RedisRegistrationCodeVerifier(
            StringRedisTemplate redisTemplate
    ) {
        this.redisTemplate = redisTemplate;

        this.verifyScript = new DefaultRedisScript<>();
        this.verifyScript.setResultType(Long.class);

        this.verifyScript.setScriptText("""
                local codeKey = KEYS[1]
                local attemptsKey = KEYS[2]

                local inputCode = ARGV[1]
                local maxAttempts = tonumber(ARGV[2])
                local lockSeconds = tonumber(ARGV[3])

                -- 1. 先检查是否已达到错误次数限制
                local attempts = tonumber(
                    redis.call('GET', attemptsKey) or '0'
                )

                if attempts >= maxAttempts then
                    return -2
                end

                -- 2. 查询验证码
                local savedCode = redis.call('GET', codeKey)

                if not savedCode then
                    return -1
                end

                -- 3. 验证码正确
                if savedCode == inputCode then

                    -- 原子删除，防止重复使用
                    redis.call('DEL', codeKey)
                    redis.call('DEL', attemptsKey)

                    return 1
                end

                -- 4. 验证码错误，增加错误次数
                local newAttempts = redis.call(
                    'INCR', attemptsKey
                )

                -- 第一次错误，设置10分钟过期
                if newAttempts == 1 then
                    redis.call(
                        'EXPIRE',
                        attemptsKey,
                        lockSeconds
                    )
                end

                -- 5. 达到限制
                if newAttempts >= maxAttempts then
                    return -2
                end

                -- 6. 普通验证码错误
                return 0
                """);
    }

    @Override
    public void verify(
            String phone,
            String verificationCode
    ) {

        String codeKey = CODE_KEY_PREFIX + phone;
        String attemptsKey = ATTEMPTS_KEY_PREFIX + phone;

        Long result = redisTemplate.execute(
                verifyScript,
                List.of(codeKey, attemptsKey),
                verificationCode,
                String.valueOf(MAX_ATTEMPTS),
                String.valueOf(LOCK_SECONDS)
        );

        switch (result.intValue()) {
            case 1 -> {
                return;
            }
            case -1 -> throw new BusinessException(
                    "验证码不存在或已过期"
            );
            case -2 -> throw new BusinessException(
                    "验证码错误次数过多，请稍后重试"
            );
            case 0 -> throw new BusinessException(
                    "验证码错误"
            );
            default -> throw new IllegalStateException(
                    "未知的验证码校验结果"
            );
        }
    }
}