package com.mall.merchant.domain.merchant.model;

/** 对应 merchant.status 列；业务流转规则待功能实现阶段补充。 */
public enum MerchantStatus {
    NORMAL(1),
    DISABLED(2);

    private final int code;

    MerchantStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
