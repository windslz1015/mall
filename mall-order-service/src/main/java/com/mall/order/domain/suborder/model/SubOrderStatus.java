package com.mall.order.domain.suborder.model;

/**
 * 商家子订单状态与主订单当前支付/取消阶段一致，不承担商家发货履约状态。
 * code 必须与当前 Flyway SQL 状态 CHECK 约束保持一致。
 */
public enum SubOrderStatus {
    CREATING(0),
PENDING_PAYMENT(10),
PAID(20),
CANCELLED(30),
CREATION_FAILED(40);

    private final int code;

    SubOrderStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
