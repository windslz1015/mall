package com.mall.iam.application.account.service;

import com.mall.common.exception.BusinessException;
import com.mall.iam.application.account.command.RegisterAccountCommand;
import com.mall.iam.application.account.port.verification.RegistrationCodeVerifier;
import com.mall.iam.application.account.result.RegisterAccountResult;
import com.mall.iam.domain.account.error.AccountErrorCode;
import com.mall.iam.domain.account.repository.AccountRepository;
import com.mall.iam.domain.account.service.PasswordPolicy;
import com.mall.iam.application.account.port.event.RegistrationEventDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** 不开启数据库事务：先占用 Redis 验证码，再调用另一个 Spring Bean 上的事务方法。 */
@Service
public class RegisterAccountApplicationService {
    private static final Logger log = LoggerFactory.getLogger(RegisterAccountApplicationService.class);
    private final RegisterAccountTransactionService transactionService;
    private final RegistrationCodeVerifier codeVerifier;
    private final PasswordPolicy passwordPolicy;
    private final AccountRepository accountRepository;
    private final RegistrationEventDispatcher outboxDispatcher;

    public RegisterAccountApplicationService(RegisterAccountTransactionService transactionService,
                                             RegistrationCodeVerifier codeVerifier,
                                             PasswordPolicy passwordPolicy,
                                             AccountRepository accountRepository,
                                             RegistrationEventDispatcher outboxDispatcher) {
        this.transactionService = transactionService;
        this.codeVerifier = codeVerifier;
        this.passwordPolicy = passwordPolicy;
        this.accountRepository = accountRepository;
        this.outboxDispatcher = outboxDispatcher;
    }

    public RegisterAccountResult register(RegisterAccountCommand input) {
        RegisterAccountCommand command = new RegisterAccountCommand(
                input.username().trim(), input.password(), input.phone().trim(),
                normalizeEmail(input.email()), trimToNull(input.nickname()),
                input.verificationCode().trim());
        passwordPolicy.check(command.password());
        // 不要把验证码先 DEL 掉：MySQL 事务可能回滚。
        String token = codeVerifier.reserve(command.phone(), command.verificationCode());
        RegistrationWriteResult writeResult;
        try {
            // 该调用通过另一个 Spring 代理；方法返回时 JDBC 事务已提交。
            writeResult = transactionService.createAccount(command);
        } catch (RuntimeException failure) {
            try {
                codeVerifier.release(command.phone(), token);
            } catch (RuntimeException cleanupError) {
                log.warn("注册失败后释放验证码租约失败，租约过期后会自动释放", cleanupError);
            }
            if (failure instanceof DuplicateKeyException) {
                // UNIQUE 约束兜住并发读写；只把已证实的用户名/手机/邮箱冲突翻译为业务异常。
                if (accountRepository.existsByUsername(command.username())) {
                    throw new BusinessException(AccountErrorCode.USERNAME_ALREADY_EXISTS);
                }
                if (accountRepository.existsByPhone(command.phone())) {
                    throw new BusinessException(AccountErrorCode.PHONE_ALREADY_EXISTS);
                }
                if (command.email() != null && accountRepository.existsByEmail(command.email())) {
                    throw new BusinessException(AccountErrorCode.EMAIL_ALREADY_EXISTS);
                }
            }
            throw failure;
        }
        // 后提交操作：任何 Redis/RabbitMQ 故障都不能改变已成功提交的注册结果。
        try {
            codeVerifier.confirm(command.phone(), token, command.verificationCode());
        } catch (RuntimeException error) {
            log.warn("注册已提交，但确认消费验证码失败；后续由 TTL/账号唯一性阻止重复注册", error);
        }
        outboxDispatcher.submit(writeResult.outboxId());
        log.info("账号注册成功 accountId={} accountNo={}",
                writeResult.account().accountId(), writeResult.account().accountNo());
        return writeResult.account();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizeEmail(String email) {
        String value = trimToNull(email);
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
}
