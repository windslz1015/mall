
    package com.mall.iam.infrastructure.redis;

    import com.mall.common.exception.BusinessException;
    import com.mall.iam.application.account.port.verification.RegistrationCodeIssueStore;
    import com.mall.iam.domain.account.error.AccountErrorCode;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.data.redis.core.StringRedisTemplate;
    import org.springframework.data.redis.core.script.DefaultRedisScript;
    import org.springframework.stereotype.Component;

    import java.util.List;

    @Component
    public class RedisRegistrationCodeIssueStore
            implements RegistrationCodeIssueStore {

        private static final int MAX_VERIFY_ATTEMPTS = 5;
        private static final int DAILY_WINDOW_SECONDS = 86400;

        private final StringRedisTemplate redis;
        private final int validSeconds;
        private final int cooldownSeconds;
        private final int pendingSeconds;
        private final int maxPerDay;

        private final DefaultRedisScript<Long> acquireScript;
        private final DefaultRedisScript<Long> activateScript;
        private final DefaultRedisScript<Long> releaseScript;

        public RedisRegistrationCodeIssueStore(
                StringRedisTemplate redis,
                @Value("${iam.registration.code-expire-seconds:300}")
                int validSeconds,
                @Value("${iam.registration.code-send-cooldown-seconds:60}")
                int cooldownSeconds,
                @Value("${iam.registration.code-send-pending-seconds:120}")
                int pendingSeconds,
                @Value("${iam.registration.code-send-max-per-day:10}")
                int maxPerDay) {

            if (validSeconds < 60
                    || cooldownSeconds < 1
                    || pendingSeconds < 1
                    || maxPerDay < 1) {
                throw new IllegalArgumentException(
                        "Invalid registration-code configuration");
            }

            this.redis = redis;
            this.validSeconds = validSeconds;
            this.cooldownSeconds = cooldownSeconds;
            this.pendingSeconds = pendingSeconds;
            this.maxPerDay = maxPerDay;

            acquireScript = script("""
                    if redis.call('EXISTS', KEYS[3]) == 1 then return -3 end
                    local attempts = tonumber(redis.call('GET', KEYS[2]) or '0')
                    if attempts >= tonumber(ARGV[1]) then return -2 end
                    if redis.call('EXISTS', KEYS[6]) == 1 then return 0 end
                    if redis.call('EXISTS', KEYS[4]) == 1 then return 0 end
                    local daily = tonumber(redis.call('GET', KEYS[5]) or '0')
                    if daily >= tonumber(ARGV[3]) then return -4 end
                    local acquired = redis.call('SET', KEYS[6], ARGV[5], 'EX', tonumber(ARGV[6]), 'NX')
                    if not acquired then return 0 end
                    redis.call('SET', KEYS[4], '1', 'EX', tonumber(ARGV[2]))
                    local count = redis.call('INCR', KEYS[5])
                    if count == 1 then redis.call('EXPIRE', KEYS[5], tonumber(ARGV[4])) end
                    return 1
                    """);
            activateScript = script("""
                    if redis.call('GET', KEYS[6]) ~= ARGV[1] then return 0 end
                    if redis.call('EXISTS', KEYS[3]) == 1 then return -1 end
                    redis.call('SET', KEYS[1], ARGV[2], 'EX', tonumber(ARGV[3]))
                    -- 新验证码对应新一轮验证次数
                    redis.call('DEL', KEYS[2])
                    -- 发送结束，释放发送锁
                    redis.call('DEL', KEYS[6])
                    return 1
                    """);

            releaseScript = script("""
                    -- 只能释放自己的发送锁
                    if redis.call('GET', KEYS[6]) ~= ARGV[1] then
                        return 0
                    end
    
                    redis.call('DEL', KEYS[6])
    
                    -- 故意不删除 cooldown 和 daily count
                    return 1
                    """);
        }

        @Override
        public void acquire(String phone, String token) {
            Long value = redis.execute(
                    acquireScript,
                    keys(phone),
                    String.valueOf(MAX_VERIFY_ATTEMPTS),
                    String.valueOf(cooldownSeconds),
                    String.valueOf(maxPerDay),
                    String.valueOf(DAILY_WINDOW_SECONDS),
                    token,
                    String.valueOf(pendingSeconds)
            );

            if (value == null) {
                throw new IllegalStateException(
                        "Redis issuance failed");
            }

            switch (value.intValue()) {
                case 1 -> {
                    return;
                }
                case 0 -> throw new BusinessException(
                        AccountErrorCode.VERIFICATION_CODE_SEND_TOO_FREQUENT);
                case -2 -> throw new BusinessException(
                        AccountErrorCode.VERIFICATION_CODE_LOCKED);
                case -3 -> throw new BusinessException(
                        AccountErrorCode.VERIFICATION_CODE_BUSY);
                case -4 -> throw new BusinessException(
                        AccountErrorCode.VERIFICATION_CODE_SEND_DAILY_LIMIT);
                default -> throw new IllegalStateException(
                        "Unexpected Redis issuance result: " + value);
            }
        }

        @Override
        public void activate(String phone, String token, String code) {

            Long value = redis.execute(
                    activateScript,
                    keys(phone),
                    token,
                    code,
                    String.valueOf(validSeconds)
            );

            if (value == null || value.longValue() != 1L) {
                throw new IllegalStateException(
                        "Could not activate the registration code");
            }
        }

        @Override
        public void release(String phone, String token) {
            Long value = redis.execute(
                    releaseScript,
                    keys(phone),
                    token
            );

            if (value == null) {
                throw new IllegalStateException(
                        "Redis sending-slot release failed");
            }
        }

        private static List<String> keys(String phone) {
            String base = "iam:register:{" + phone + "}";

            return List.of(
                    base + ":code",
                    base + ":attempts",
                    base + ":lease",
                    base + ":send_cooldown",
                    base + ":daily_send_count",
                    base + ":send_pending"
            );
        }

        private static DefaultRedisScript<Long> script(String lua) {
            DefaultRedisScript<Long> script =
                    new DefaultRedisScript<>();

            script.setScriptText(lua);
            script.setResultType(Long.class);

            return script;
        }
    }
