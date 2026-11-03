package com.mall.payment.domain.attempt.model;

/** 支付尝试状态；与 payment_attempt.status 一致。 */
public enum PaymentAttemptStatus {
    CREATED(0),
    PROCESSING(10),
    SUCCEEDED(20),
    FAILED(30),
    CLOSED(40);

    private final int code;
    PaymentAttemptStatus(int code) { this.code = code; }
    public int getCode() { return code; }
}
