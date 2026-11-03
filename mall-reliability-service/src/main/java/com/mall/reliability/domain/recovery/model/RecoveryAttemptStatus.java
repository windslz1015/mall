package com.mall.reliability.domain.recovery.model;

/** 单次补偿执行状态；与 reliability_recovery_attempt.status 对齐。 */
public enum RecoveryAttemptStatus {
    STARTED(0),
    SUCCEEDED(10),
    FAILED(20),
    TIMED_OUT(30);

    private final int code;
    RecoveryAttemptStatus(int code) { this.code = code; }
    public int getCode() { return code; }
}
