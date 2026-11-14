package com.mall.iam.application.account.port.authorization;
import com.mall.iam.application.account.query.GrantedLoginAuthorization;
public interface LoginAuthorizationPort {
    GrantedLoginAuthorization loadNormalUserAuthorization(Long accountId);
}
