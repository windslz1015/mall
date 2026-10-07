package com.mall.iam.domain.account.model;

public enum AccountType {

    NORMAL_USER(1),
    MERCHANT(2),
    PLATFORM_ADMIN(3);

    private final int code;

    AccountType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
