package com.mall.iam.application.account.port.persistence;

import com.mall.iam.application.account.query.LoginAccount;
import java.time.LocalDateTime;
import java.util.Optional;

/** Login-only storage contract; domain account registration repository stays unchanged. */
public interface LoginAccountStore {
    Optional<LoginAccount> findByUsernameForUpdate(String username);
    Optional<LoginAccount> findByPhoneForUpdate(String phone);
    Optional<LoginAccount> findById(Long accountId);
    void updateFailureState(Long accountId, int failures, LocalDateTime lockedUntil);
    void updateSuccessfulLogin(Long accountId, String clientIp, LocalDateTime nowUtc);
}
