
package com.mall.gateway.service.register;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class RegisterRiskService {

    private final ReactiveStringRedisTemplate redis;

    private final DefaultRedisScript<Long> script;

    private static final String LUA = """
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """;

    public RegisterRiskService(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
        this.script = new DefaultRedisScript<>();
        this.script.setScriptText(LUA);
        this.script.setResultType(Long.class);
    }

    // 检查IP注册频率：10分钟最多20次
    public Mono<Boolean> checkIp(String ip) {
        String key = "risk:register:ip:" + ip;
        return checkLimit(key, 600, 20);
    }

    //检查手机号注册频率：1小时最多5次，参数必须是服务端生成的不可逆HMAC标识
    public Mono<Boolean> checkPhoneKey(String phoneKey) {
        String key = "risk:register:phone:" + phoneKey;
        return checkLimit(key, 3600, 5);
    }

    //限流方法：redis计数，拿到计数值出来判断是否超过上限。
    private Mono<Boolean> checkLimit(String key, int seconds, int maxCount) {
        return redis.execute(script, List.of(key), List.of(String.valueOf(seconds))).next().map(count -> count <= maxCount);
    }
}