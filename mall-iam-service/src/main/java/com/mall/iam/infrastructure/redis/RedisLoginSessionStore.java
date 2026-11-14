package com.mall.iam.infrastructure.redis;

import com.mall.iam.application.account.port.session.LoginSessionStore;
import com.mall.iam.application.account.query.LoginSession;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Lua is used for creation, refresh rotation and logout to avoid read-modify-write races. */
@Repository
public class RedisLoginSessionStore implements LoginSessionStore {
    private static final String PREFIX = "iam:auth:session:";
    private static final DefaultRedisScript<Long> CREATE = script("""
            if redis.call('EXISTS', KEYS[1]) == 1 then return 0 end
            redis.call('HSET', KEYS[1], 'accountId', ARGV[1], 'refreshHash', ARGV[2],
                       'status', 'ACTIVE', 'expiresAt', ARGV[4])
            redis.call('EXPIRE', KEYS[1], ARGV[3])
            return 1
            """);
    private static final DefaultRedisScript<Long> ROTATE = script("""
            if redis.call('HGET', KEYS[1], 'status') ~= 'ACTIVE' then return 0 end
            if redis.call('HGET', KEYS[1], 'accountId') ~= ARGV[1] then return 0 end
            if redis.call('HGET', KEYS[1], 'refreshHash') ~= ARGV[2] then return 0 end
            redis.call('HSET', KEYS[1], 'refreshHash', ARGV[3], 'expiresAt', ARGV[5])
            redis.call('EXPIRE', KEYS[1], ARGV[4])
            return 1
            """);
    private static final DefaultRedisScript<Long> REVOKE = script("""
            if redis.call('HGET', KEYS[1], 'refreshHash') ~= ARGV[1] then return 0 end
            return redis.call('DEL', KEYS[1])
            """);
    private final StringRedisTemplate redis;

    public RedisLoginSessionStore(StringRedisTemplate redis) { this.redis = redis; }

    private static DefaultRedisScript<Long> script(String lua) {
        DefaultRedisScript<Long> s = new DefaultRedisScript<>();
        s.setScriptText(lua);
        s.setResultType(Long.class);
        return s;
    }

    private static String key(String sid) { return PREFIX + sid; }

    @Override
    public void create(String sid, Long accountId, String refreshHash, long ttlSeconds) {
        Long value = redis.execute(CREATE, List.of(key(sid)), accountId.toString(), refreshHash,
                Long.toString(ttlSeconds), Long.toString(Instant.now().plusSeconds(ttlSeconds).getEpochSecond()));
        if (!Long.valueOf(1L).equals(value)) throw new IllegalStateException("create login session failed");
    }

    @Override
    public Optional<LoginSession> findActive(String sid) {
        Map<Object, Object> values = redis.opsForHash().entries(key(sid));
        if (!"ACTIVE".equals(values.get("status"))) return Optional.empty();
        Object id = values.get("accountId");
        Object hash = values.get("refreshHash");
        if (id == null || hash == null) return Optional.empty();
        try {
            return Optional.of(new LoginSession(sid, Long.valueOf(id.toString()), hash.toString()));
        } catch (NumberFormatException ex) { return Optional.empty(); }
    }

    @Override
    public boolean rotate(String sid, Long accountId, String oldHash, String newHash, long ttlSeconds) {
        Long value = redis.execute(ROTATE, List.of(key(sid)), accountId.toString(), oldHash,
                newHash, Long.toString(ttlSeconds),
                Long.toString(Instant.now().plusSeconds(ttlSeconds).getEpochSecond()));
        return Long.valueOf(1L).equals(value);
    }

    @Override
    public void revokeIfMatches(String sid, String hash) {
        redis.execute(REVOKE, List.of(key(sid)), hash);
    }

    @Override
    public void revokeSession(String sid) { redis.delete(key(sid)); }
}
