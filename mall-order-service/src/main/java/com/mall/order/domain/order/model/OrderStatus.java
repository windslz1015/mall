package com.mall.order.domain.order.model;

/**
 * 交易主订单状态：0创建中；10待支付；20已支付；30已取消；40创建失败。当前未建物流/售后状态。
 * code 必须与当前 Flyway SQL 状态 CHECK 约束保持一致。
 */
public enum OrderStatus {
    CREATING(0),
PENDING_PAYMENT(10),
PAID(20),
CANCELLED(30),
CREATION_FAILED(40);

    private final int code;

    OrderStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
