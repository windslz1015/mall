package com.mall.iam.infrastructure.generator;

import com.mall.iam.application.account.port.identity.AccountNoGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class SnowflakeAccountNoGenerator
        implements AccountNoGenerator {

    //Snowflake ID：0 | 41位时间戳 | 10位workerId | 12位sequence
    //自定义起始时间：2024-01-01 UTC
    private static final long EPOCH = 1704067200000L;

    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID =
            (1L << WORKER_ID_BITS) - 1;

    private static final long MAX_SEQUENCE =
            (1L << SEQUENCE_BITS) - 1;

    private static final long WORKER_ID_SHIFT =
            SEQUENCE_BITS;

    private static final long TIMESTAMP_SHIFT =
            WORKER_ID_BITS + SEQUENCE_BITS;

    private final long workerId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeAccountNoGenerator(
            @Value("${iam.snowflake.worker-id}") long workerId) {

        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    "workerId 必须在 0-1023 之间"
            );
        }

        this.workerId = workerId;
    }

    @Override
    public String next() {
        return "AC" + nextId();
    }


    public synchronized long nextId() {

        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new IllegalStateException(
                    "系统时钟回拨，拒绝生成账号编号"
            );
        }

        if (timestamp == lastTimestamp) {

            sequence = (sequence + 1) & MAX_SEQUENCE;

            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }

        } else {
            sequence = 0L;
        }

        long elapsed = timestamp - EPOCH;

        if (elapsed < 0 || elapsed >= (1L << 41)) {
            throw new IllegalStateException(
                    "时间戳超出 Snowflake 可用范围"
            );
        }

        lastTimestamp = timestamp;

        long id =
                (elapsed << TIMESTAMP_SHIFT)
                        | (workerId << WORKER_ID_SHIFT)
                        | sequence;

        return id;
    }

    private long waitNextMillis(long previousTimestamp) {

        long timestamp = System.currentTimeMillis();

        while (timestamp <= previousTimestamp) {
            timestamp = System.currentTimeMillis();
        }

        return timestamp;
    }
}