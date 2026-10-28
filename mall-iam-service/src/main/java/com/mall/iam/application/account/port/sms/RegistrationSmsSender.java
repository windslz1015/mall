package com.mall.iam.application.account.port.sms;

/** Implement with a real SMS provider in non-development environments. */
public interface RegistrationSmsSender {
    void sendRegistrationCode(String phone, String code, int validSeconds);
}
