package com.mall.gateway.service.login;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.List;

@Service
public class LoginRiskService {
    private static final DefaultRedisScript<Long> CHECK = new DefaultRedisScript<>();
    static {
        CHECK.setScriptText("""
                local n = redis.call('INCR', KEYS[1])
                if n == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
                return n
                """);
        CHECK.setResultType(Long.class);
    }
    private final ReactiveStringRedisTemplate redis;
    private final int maxRequestsPerMinute;

    public LoginRiskService(ReactiveStringRedisTemplate redis,
            @Value("${gateway.login.risk.max-ip-requests-per-minute:30}") int maxRequestsPerMinute) {
        if (maxRequestsPerMinute < 1) throw new IllegalArgumentException("invalid login IP limit");
        this.redis = redis;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    public Mono<Boolean> checkIp(String ip) {
        return redis.execute(CHECK, List.of("risk:login:ip:" + ip), List.of("60"))
                .next().map(value -> value <= maxRequestsPerMinute).defaultIfEmpty(false);
    }
}
