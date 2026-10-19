package com.mall.iam.application.account.service;

import com.mall.iam.application.account.command.RegisterAccountCommand;
import com.mall.iam.application.account.port.authorization.AccountRoleAssignment;
import com.mall.iam.application.account.port.identity.AccountNoGenerator;
import com.mall.iam.application.account.port.security.PasswordHasher;
import com.mall.iam.application.account.result.RegisterAccountResult;
import com.mall.iam.domain.account.model.Account;
import com.mall.iam.domain.account.repository.AccountRepository;
import com.mall.iam.domain.account.service.AccountRegistrationPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterAccountTransactionService {
    private final AccountRepository accountRepository;
    private final AccountRegistrationPolicy registrationPolicy;
    private final PasswordHasher passwordHasher;
    private final AccountNoGenerator accountNoGenerator;
    private final AccountRoleAssignment accountRoleAssignment;
    private final AccountRegistrationEventRecorder eventRecorder;

    public RegisterAccountTransactionService(AccountRepository accountRepository,
                                             AccountRegistrationPolicy registrationPolicy,
                                             PasswordHasher passwordHasher,
                                             AccountNoGenerator accountNoGenerator,
                                             AccountRoleAssignment accountRoleAssignment,
                                             AccountRegistrationEventRecorder eventRecorder) {
        this.accountRepository = accountRepository;
        this.registrationPolicy = registrationPolicy;
        this.passwordHasher = passwordHasher;
        this.accountNoGenerator = accountNoGenerator;
        this.accountRoleAssignment = accountRoleAssignment;
        this.eventRecorder = eventRecorder;
    }

    /** 四次数据库写操作必须在同一 DataSourceTransactionManager 管理的 MySQL 事务内。 */
    @Transactional(rollbackFor = Exception.class)
    public RegistrationWriteResult createAccount(RegisterAccountCommand command) {
        registrationPolicy.check(command.username(), command.phone(), command.email());
        String passwordHash = passwordHasher.hash(command.password());
        Account account = Account.registerNormalUser(accountNoGenerator.next(), command.username(),
                command.phone(), command.email(), passwordHash, command.nickname());
        accountRepository.save(account); // 依次写 iam_account、iam_account_profile
        accountRoleAssignment.assignDefaultRole(account.getId()); // sys_account_role
        Long eventId = eventRecorder.record(account); // outbox_event（PENDING）
        return new RegistrationWriteResult(
                new RegisterAccountResult(account.getId(), account.getAccountNo(), account.getUsername()), eventId);
    }
}
