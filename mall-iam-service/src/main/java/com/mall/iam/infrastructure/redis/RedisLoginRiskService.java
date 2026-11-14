package com.mall.iam.infrastructure.redis;

import com.mall.common.exception.BusinessException;
import com.mall.iam.application.account.port.risk.LoginRiskPort;
import com.mall.iam.domain.account.error.AccountErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
public class RedisLoginRiskService implements LoginRiskPort {
    private static final DefaultRedisScript<Long> REQUEST = new DefaultRedisScript<>();
    private static final DefaultRedisScript<Long> FAILURE = new DefaultRedisScript<>();
    static {
        REQUEST.setScriptText("""
                local num = redis.call('INCR', KEYS[1])
                if num == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
                if num > tonumber(ARGV[2]) then return 0 end
                local failures = redis.call('GET', KEYS[2])
                if failures and tonumber(failures) >= tonumber(ARGV[3]) then return 0 end
                return 1
                """);
        REQUEST.setResultType(Long.class);
        FAILURE.setScriptText("""
                local num = redis.call('INCR', KEYS[1])
                if num == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
                return num
                """);
        FAILURE.setResultType(Long.class);
    }
    private final StringRedisTemplate redis;
    private final int maxRequests;
    private final int maxBadCredentials;

    public RedisLoginRiskService(StringRedisTemplate redis,
            @Value("${iam.login.risk.max-requests-per-minute:20}") int maxRequests,
            @Value("${iam.login.risk.max-bad-credentials-per-window:10}") int maxBadCredentials) {
        if (maxRequests <= 0 || maxBadCredentials <= 0) throw new IllegalArgumentException("invalid login risk config");
        this.redis = redis;
        this.maxRequests = maxRequests;
        this.maxBadCredentials = maxBadCredentials;
    }

    private String prefix(String identifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(identifier.getBytes(StandardCharsets.UTF_8));
            return "risk:login:identifier:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    @Override
    public void beforeLogin(String normalizedIdentifier) {
        String p = prefix(normalizedIdentifier);
        Long allowed = redis.execute(REQUEST, List.of(p + ":requests", p + ":failures"),
                "60", Integer.toString(maxRequests), Integer.toString(maxBadCredentials));
        if (!Long.valueOf(1L).equals(allowed)) {
            throw new BusinessException(AccountErrorCode.LOGIN_TOO_FREQUENT);
        }
    }

    @Override
    public void recordBadCredentials(String normalizedIdentifier) {
        redis.execute(FAILURE, List.of(prefix(normalizedIdentifier) + ":failures"), "900");
    }

    @Override
    public void clearFailures(String normalizedIdentifier) {
        redis.delete(prefix(normalizedIdentifier) + ":failures");
    }
}
