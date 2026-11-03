package com.mall.order.domain.cancellation.model;

/**
 * 取消来源：1用户取消、2支付超时、3系统异常取消；只适用于原待支付订单。
 * code 必须与当前 Flyway SQL 状态 CHECK 约束保持一致。
 */
public enum OrderCancellationType {
    USER(1),
PAYMENT_TIMEOUT(2),
SYSTEM_EXCEPTION(3);

    private final int code;

    OrderCancellationType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
