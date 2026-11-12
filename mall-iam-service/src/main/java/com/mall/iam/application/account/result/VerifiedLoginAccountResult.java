package com.mall.iam.application.account.result;

/** 已通过账号与密码校验；不包含密码哈希、Token 或登录会话。 */
public record VerifiedLoginAccountResult(Long accountId, String accountNo, String username) {
}