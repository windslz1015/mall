package com.mall.merchant.domain.merchant.model;

/** 对应 merchant_account_binding.binding_role；本阶段仅作数据字典。 */
public enum MerchantAccountBindingRole {
    OWNER(1),
    OPERATOR(2);

    private final int code;

    MerchantAccountBindingRole(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
