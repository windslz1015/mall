package com.mall.iam.application.account.port.risk;
public interface LoginRiskPort {
    void beforeLogin(String normalizedIdentifier);
    void recordBadCredentials(String normalizedIdentifier);
    void clearFailures(String normalizedIdentifier);
}
