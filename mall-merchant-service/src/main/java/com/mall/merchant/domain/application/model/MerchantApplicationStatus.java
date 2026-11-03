package com.mall.merchant.domain.application.model;

/** 对应 merchant_application.status 列。 */
public enum MerchantApplicationStatus {
    PENDING(1),
    APPROVED(2),
    REJECTED(3);

    private final int code;

    MerchantApplicationStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
