package com.mall.payment.domain.record.model;

/** 支付渠道已确认入账事实的业务复核状态；与 payment_record.review_status 一致。 */
public enum PaymentReviewStatus {
    NORMAL(0),
    LATE_SUCCESS(10),
    DUPLICATE_SUCCESS(20),
    AMOUNT_MISMATCH(30),
    NEEDS_REVIEW(40);

    private final int code;
    PaymentReviewStatus(int code) { this.code = code; }
    public int getCode() { return code; }
}
