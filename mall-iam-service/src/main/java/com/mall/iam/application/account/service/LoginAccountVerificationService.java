package com.mall.iam.application.account.service;

import com.mall.common.exception.BusinessException;
import com.mall.iam.application.account.command.VerifyLoginAccountCommand;
import com.mall.iam.application.account.port.security.PasswordHasher;
import com.mall.iam.application.account.result.VerifiedLoginAccountResult;
import com.mall.iam.domain.account.error.AccountErrorCode;
import com.mall.iam.domain.account.model.AccountStatus;
import com.mall.iam.domain.account.model.AccountType;
import com.mall.iam.domain.account.model.LoginAccount;
import com.mall.iam.domain.account.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.regex.Pattern;

/** 仅负责登录账号查询及凭证校验；Token、会话和登录日志由后续流程接入。 */
@Service
public class LoginAccountVerificationService {

    private static final Pattern CHINESE_PHONE = Pattern.compile("^1[3-9]\\d{9}$");

    private final AccountRepository accountRepository;
    private final PasswordHasher passwordHasher;
    private final int maxFailedAttempts;
    private final long lockDurationMinutes;

    public LoginAccountVerificationService(
            AccountRepository accountRepository,
            PasswordHasher passwordHasher,
            @Value("${iam.login.max-failed-attempts:5}") int maxFailedAttempts,
            @Value("${iam.login.lock-duration-minutes:15}") long lockDurationMinutes) {
        if (maxFailedAttempts < 1 || lockDurationMinutes < 1) {
            throw new IllegalArgumentException("登录失败阈值和锁定时长必须为正数");
        }
        this.accountRepository = accountRepository;
        this.passwordHasher = passwordHasher;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockDurationMinutes = lockDurationMinutes;
    }

    /**
     * SELECT ... FOR UPDATE 保证同账号并发登录不会覆盖失败次数。
     * REQUIRES_NEW 使失败次数独立提交，不受未来外层登录流程回滚影响；
     * noRollbackFor 则保证抛出账号密码错误时仍提交失败计数。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = BusinessException.class)
    public VerifiedLoginAccountResult verify(VerifyLoginAccountCommand command) {
        if (command == null || command.loginIdentifier() == null
                || command.loginIdentifier().isBlank()
                || command.password() == null || command.password().isEmpty()) {
            throw new BusinessException(AccountErrorCode.LOGIN_BAD_CREDENTIALS);
        }

        String identifier = command.loginIdentifier().trim();
        // 避免超长标识与超长 BCrypt 密码占用不必要的查询/计算资源。
        if (identifier.length() > 64
                || command.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(AccountErrorCode.LOGIN_BAD_CREDENTIALS);
        }
        Optional<LoginAccount> accountOptional;
        if (CHINESE_PHONE.matcher(identifier).matches()) {
            // 当前注册规则允许 11 位数字用户名，因此手机号格式可能同时命中两个不同账号。
            // 固定先查 username 再查 phone；跨字段撞号时拒绝，避免认证到错误账号。
            Optional<LoginAccount> byUsername = accountRepository.findForLoginByUsernameForUpdate(identifier);
            Optional<LoginAccount> byPhone = accountRepository.findForLoginByPhoneForUpdate(identifier);
            if (byUsername.isPresent() && byPhone.isPresent()
                    && !byUsername.get().accountId().equals(byPhone.get().accountId())) {
                throw new BusinessException(AccountErrorCode.LOGIN_BAD_CREDENTIALS);
            }
            accountOptional = byPhone.isPresent() ? byPhone : byUsername;
        } else {
            accountOptional = accountRepository.findForLoginByUsernameForUpdate(identifier);
        }

        // 不区分“账号不存在”和“密码错误”，防止通过业务提示枚举账号。
        LoginAccount account = accountOptional.orElseThrow(
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
            // 上一个临时锁定期已经到期：重新开始计数，而不是立即再次锁定。
            int previousFailures = account.hasExpiredTemporaryLockAt(nowUtc)
                    ? 0 : account.failedLoginCount();
            int newFailures = Math.min(previousFailures + 1, maxFailedAttempts);
            LocalDateTime newLockedUntil = newFailures >= maxFailedAttempts
                    ? nowUtc.plusMinutes(lockDurationMinutes) : null;
            accountRepository.updateLoginFailureState(account.accountId(), newFailures, newLockedUntil);
            throw new BusinessException(AccountErrorCode.LOGIN_BAD_CREDENTIALS);
        }

        // 密码正确：重置失败次数，清除过期的临时锁定。
        accountRepository.updateLoginFailureState(account.accountId(), 0, null);
        return new VerifiedLoginAccountResult(
                account.accountId(), account.accountNo(), account.username());
    }
}