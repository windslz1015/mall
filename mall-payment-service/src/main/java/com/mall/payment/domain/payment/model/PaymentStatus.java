package com.mall.payment.domain.payment.model;

/** 支付单状态；必须与 payment_order.status 的数据库 CHECK 保持一致。 */
public enum PaymentStatus {
    CREATED(0),
    PENDING(10),
    SUCCEEDED(20),
    FAILED(30),
    CLOSED(40),
    EXCEPTION(50);

    private final int code;
    PaymentStatus(int code) { this.code = code; }
    public int getCode() { return code; }
}
