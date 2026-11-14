package com.mall.iam.application.account.service;

import com.mall.common.exception.BusinessException;
import com.mall.iam.application.account.command.VerifyLoginAccountCommand;
import com.mall.iam.application.account.port.persistence.LoginAccountStore;
import com.mall.iam.application.account.port.security.PasswordHasher;
import com.mall.iam.application.account.query.LoginAccount;
import com.mall.iam.application.account.result.VerifiedLoginAccountResult;
import com.mall.iam.domain.account.error.AccountErrorCode;
import com.mall.iam.domain.account.model.AccountStatus;
import com.mall.iam.domain.account.model.AccountType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class LoginAccountVerificationService {
    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private final LoginAccountStore store;
    private final PasswordHasher passwordHasher;
    private final int maxFailures;
    private final int lockMinutes;

    public LoginAccountVerificationService(LoginAccountStore store, PasswordHasher passwordHasher,
            @Value("${iam.login.max-failed-attempts:5}") int maxFailures,
            @Value("${iam.login.lock-duration-minutes:15}") int lockMinutes) {
        if (maxFailures <= 0 || lockMinutes <= 0) throw new IllegalArgumentException("invalid login lock config");
        this.store = store;
        this.passwordHasher = passwordHasher;
        this.maxFailures = maxFailures;
        this.lockMinutes = lockMinutes;
    }

    /** Keep the row lock until failed-attempt update is committed, including business errors. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = BusinessException.class)
    public VerifiedLoginAccountResult verify(VerifyLoginAccountCommand command) {
        if (command == null || command.loginIdentifier() == null || command.loginIdentifier().isBlank()
                || command.password() == null || command.password().isEmpty()) {
            throw new BusinessException(AccountErrorCode.LOGIN_BAD_CREDENTIALS);
        }
        String identifier = command.loginIdentifier().trim();
        if (identifier.length() > 64 || command.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(AccountErrorCode.LOGIN_BAD_CREDENTIALS);
        }
        Optional<LoginAccount> selected;
        if (PHONE.matcher(identifier).matches()) {
            // A phone-shaped username may coexist with a different account's phone number.
            Optional<LoginAccount> username = store.findByUsernameForUpdate(identifier);
            Optional<LoginAccount> phone = store.findByPhoneForUpdate(identifier);
            if (username.isPresent() && phone.isPresent()
                    && !username.get().accountId().equals(phone.get().accountId())) {
                throw new BusinessException(AccountErrorCode.LOGIN_BAD_CREDENTIALS);
            }
            selected = phone.isPresent() ? phone : username;
        } else {
            selected = store.findByUsernameForUpdate(identifier);
        }
        LoginAccount account = selected.orElseThrow(
                () -> new BusinessException(AccountErrorCode.LOGIN_BAD_CREDENTIALS));
        if (account.accountType() != AccountType.NORMAL_USER) {
            throw new BusinessException(AccountErrorCode.LOGIN_BAD_CREDENTIALS);
        }
        if (account.status() == AccountStatus.LOCKED) {
            throw new BusinessException(AccountErrorCode.LOGIN_ACCOUNT_LOCKED);
        }
        if (account.status() != AccountStatus.NORMAL) {
            throw new BusinessException(AccountErrorCode.LOGIN_ACCOUNT_UNAVAILABLE);
        }
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        if (account.isTemporarilyLockedAt(nowUtc)) {
            throw new BusinessException(AccountErrorCode.LOGIN_ACCOUNT_LOCKED);
        }
        if (!passwordHasher.matches(command.password(), account.passwordHash())) {
            int current = account.hasExpiredTemporaryLockAt(nowUtc) ? 0 : account.failedLoginCount();
            int failures = Math.min(maxFailures, current + 1);
            store.updateFailureState(account.accountId(), failures,
                    failures >= maxFailures ? nowUtc.plusMinutes(lockMinutes) : null);
            throw new BusinessException(AccountErrorCode.LOGIN_BAD_CREDENTIALS);
        }
        store.updateFailureState(account.accountId(), 0, null);
        return new VerifiedLoginAccountResult(account.accountId(), account.accountNo(), account.username());
    }
}
