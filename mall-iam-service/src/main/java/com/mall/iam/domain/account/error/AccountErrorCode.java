package com.mall.iam.domain.account.error;

import com.mall.common.api.ErrorCode;

public enum AccountErrorCode implements ErrorCode {
    USERNAME_ALREADY_EXISTS(10001, "用户名已被注册"),
    PHONE_ALREADY_EXISTS(10002, "手机号已被注册"),
    EMAIL_ALREADY_EXISTS(10003, "邮箱已被注册"),
    PASSWORD_EMPTY(10004, "密码不能为空"),
    PASSWORD_LENGTH_INVALID(10005, "密码长度必须为8-64位"),
    VERIFICATION_CODE_EXPIRED(10006, "验证码不存在或已过期"),
    VERIFICATION_CODE_INVALID(10007, "验证码错误"),
    VERIFICATION_CODE_LOCKED(10008, "验证码错误次数过多，请稍后再试"),
    VERIFICATION_CODE_BUSY(10009, "验证码正在验证中，请勿重复提交"),
    PASSWORD_BYTES_EXCEEDED(10010, "密码UTF-8编码后不能超过72字节"),
    VERIFICATION_CODE_SEND_TOO_FREQUENT(10011, "验证码发送太频繁，请稍后重试"),
    VERIFICATION_CODE_SEND_DAILY_LIMIT(10012, "验证码发送次数已达上限，请稍后再试"),
    PHONE_FORMAT_INVALID(10013, "手机号格式不正确"),
    LOGIN_BAD_CREDENTIALS(10014, "账号或密码错误"),
    LOGIN_ACCOUNT_UNAVAILABLE(10015, "账号状态异常，无法登录"),
    LOGIN_ACCOUNT_LOCKED(10016, "账号已锁定，请稍后重试"),
    LOGIN_TOO_FREQUENT(10017, "登录尝试过于频繁，请稍后重试"),
    LOGIN_REFRESH_INVALID(10018, "登录已失效，请重新登录");


    private final int code;
    private final String message;
    AccountErrorCode(int code, String message) {this.code = code; this.message = message;}
    @Override public int getCode() {return code;}
    @Override public String getMessage() {return message;}
}
