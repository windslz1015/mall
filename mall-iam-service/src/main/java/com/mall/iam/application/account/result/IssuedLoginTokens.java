package com.mall.iam.application.account.result;
/** The opaque refresh token is only sent via an HttpOnly cookie, never JSON. */
public record IssuedLoginTokens(NormalUserLoginResult response, String refreshToken) {}
