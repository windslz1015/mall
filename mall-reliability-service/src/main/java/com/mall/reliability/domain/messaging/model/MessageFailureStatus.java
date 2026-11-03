package com.mall.reliability.domain.messaging.model;

/** 失败消息追踪状态；与 reliability_message_failure.status 对齐。 */
public enum MessageFailureStatus {
    OPEN(0),
    RETRY_SCHEDULED(10),
    RECOVERED(20),
    IGNORED(30);

    private final int code;
    MessageFailureStatus(int code) { this.code = code; }
    public int getCode() { return code; }
}
