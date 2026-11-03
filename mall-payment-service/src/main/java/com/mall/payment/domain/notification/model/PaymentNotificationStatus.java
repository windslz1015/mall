package com.mall.payment.domain.notification.model;

/** 渠道通知处理状态；与 payment_notification.process_status 一致。 */
public enum PaymentNotificationStatus {
    RECEIVED(0),
    VERIFIED(10),
    PROCESSED(20),
    REJECTED(30),
    FAILED(40);

    private final int code;
    PaymentNotificationStatus(int code) { this.code = code; }
    public int getCode() { return code; }
}
