package com.mall.order.domain.operation.model;

/**
 * 需幂等控制的操作类型；操作名与唯一请求键共同识别同一业务操作。
 * code 必须与当前 Flyway SQL 状态 CHECK 约束保持一致。
 */
public enum OrderOperationType {
    SUBMIT(1),
USER_CANCEL(2),
TIMEOUT_CANCEL(3),
APPLY_PAYMENT_SUCCESS(4),
APPLY_PAYMENT_FAILURE(5),
COMPENSATE(6);

    private final int code;

    OrderOperationType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
