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

    public RegisterAccountTransactionService(
            AccountRepository accountRepository,
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

    @Transactional(rollbackFor = Exception.class)
    public RegisterAccountResult createAccount(
            RegisterAccountCommand command) {

        // 1. 账号唯一性预检查
        registrationPolicy.check(
                command.username(),
                command.phone(),
                command.email()
        );

        // 2. BCrypt密码哈希
        String passwordHash =
                passwordHasher.hash(command.password());

        // 3. 生成账号编号
        String accountNo = accountNoGenerator.next();

        // 4. 创建领域账号对象
        Account account = Account.registerNormalUser(
                accountNo,
                command.username(),
                command.phone(),
                command.email(),
                passwordHash,
                command.nickname()
        );

        // 5. 写入 iam_account 和 iam_account_profile
        accountRepository.save(account);

        // 6. 写入 sys_account_role
        accountRoleAssignment.assignDefaultRole(
                account.getId()
        );

        // 7. 写入 outbox_event
        eventRecorder.record(account);

        // 8. 返回注册结果
        return new RegisterAccountResult(
                account.getId(),
                account.getAccountNo(),
                account.getUsername()
        );
    }
}