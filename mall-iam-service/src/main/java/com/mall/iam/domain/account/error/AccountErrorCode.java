package com.mall.iam.domain.account.error;


import com.mall.common.api.ErrorCode;

public enum AccountErrorCode implements ErrorCode {

    USERNAME_ALREADY_EXISTS(10001, "用户名已被注册"),
    PHONE_ALREADY_EXISTS(10002, "手机号已被注册"),
    EMAIL_ALREADY_EXISTS(10003, "邮箱已被注册"),


    PASSWORD_EMPTY(10004, "密码不能为空"),
    PASSWORD_LENGTH_INVALID(10005, "密码长度必须为8-64位");

    private final int code;
    private final String message;

    AccountErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
