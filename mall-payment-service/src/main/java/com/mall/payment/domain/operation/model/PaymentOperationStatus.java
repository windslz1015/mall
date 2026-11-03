package com.mall.payment.domain.operation.model;

/** 操作幂等状态；与 payment_operation.status 一致。 */
public enum PaymentOperationStatus {
    PROCESSING(1),
    SUCCEEDED(2),
    FAILED(3);

    private final int code;
    PaymentOperationStatus(int code) { this.code = code; }
    public int getCode() { return code; }
}
