package com.mall.iam.domain.account.model;

public enum AccountStatus {

    NORMAL(1),
    DISABLED(2),
    LOCKED(3),
    CANCELLED(4);

    private final int code;

    AccountStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
