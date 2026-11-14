package com.mall.iam.application.account.port.security;

import com.mall.iam.application.account.query.GrantedLoginAuthorization;
public interface TokenProvider {
    String issueAccessToken(Long accountId, String sessionId, GrantedLoginAuthorization authorization);
    long accessTokenSeconds();
}
