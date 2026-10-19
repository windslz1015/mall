package com.mall.iam.infrastructure.generator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdGenerator {

    private static final long EPOCH = 1704067200000L;

    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID =
            (1L << WORKER_ID_BITS) - 1;

    private static final long MAX_SEQUENCE =
            (1L << SEQUENCE_BITS) - 1;

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    private static final long TIMESTAMP_SHIFT =
            WORKER_ID_BITS + SEQUENCE_BITS;

    private final long workerId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(
            @Value("${iam.snowflake.worker-id}") long workerId) {

        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    "workerId 必须在 0-1023 之间");
        }

        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new IllegalStateException(
                    "系统时钟回拨，拒绝生成 ID");
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
                    "时间戳超出 Snowflake 可用范围");
        }

        lastTimestamp = timestamp;

        return (elapsed << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long previousTimestamp) {
        long timestamp = System.currentTimeMillis();

        while (timestamp <= previousTimestamp) {
            timestamp = System.currentTimeMillis();
        }

        return timestamp;
    }
}