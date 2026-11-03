package com.mall.merchant.domain.merchant.model;

/** 对应 merchant_account_binding.status；本阶段仅作数据字典。 */
public enum MerchantAccountBindingStatus {
    ACTIVE(1),
    DISABLED(2);

    private final int code;

    MerchantAccountBindingStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
