package com.mall.iam.application.account.port.verification;

public interface RegistrationCodeIssueStore {

    void acquire(String phone, String token);

    void activate(String phone, String token, String code);

    void release(String phone, String token);
}
