package com.mall.iam.application.account.command;

/** 登录标识支持用户名或中国大陆手机号；密码保持原样，不进行 trim。 */
public record VerifyLoginAccountCommand(String loginIdentifier, String password) {
}