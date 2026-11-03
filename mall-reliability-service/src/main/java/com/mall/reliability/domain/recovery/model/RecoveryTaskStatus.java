package com.mall.reliability.domain.recovery.model;

/** 补偿/重试任务状态；与 reliability_recovery_task.status 对齐。 */
public enum RecoveryTaskStatus {
    PENDING(0),
    RUNNING(10),
    RETRY_WAIT(20),
    SUCCEEDED(30),
    EXHAUSTED(40),
    CANCELLED(50);

    private final int code;
    RecoveryTaskStatus(int code) { this.code = code; }
    public int getCode() { return code; }
}
