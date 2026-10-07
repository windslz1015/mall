package com.mall.iam.application.account.service;

import com.mall.iam.application.account.command.RegisterAccountCommand;
import com.mall.iam.application.account.port.AccountNoGenerator;
import com.mall.iam.application.account.port.PasswordHasher;
import com.mall.iam.application.account.result.RegisterAccountResult;
import com.mall.iam.domain.account.model.Account;
import com.mall.iam.domain.account.repository.AccountRepository;
import com.mall.iam.domain.account.service.AccountRegistrationPolicy;
import com.mall.iam.domain.account.service.PasswordPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterAccountApplicationService {

    private final AccountRepository accountRepository;
    private final AccountRegistrationPolicy registrationPolicy;
    private final PasswordPolicy passwordPolicy;
    private final PasswordHasher passwordHasher;
    private final AccountNoGenerator accountNoGenerator;

    public RegisterAccountApplicationService(
            AccountRepository accountRepository,
            AccountRegistrationPolicy registrationPolicy,
            PasswordPolicy passwordPolicy,
            PasswordHasher passwordHasher,
            AccountNoGenerator accountNoGenerator) {

        this.accountRepository = accountRepository;
        this.registrationPolicy = registrationPolicy;
        this.passwordPolicy = passwordPolicy;
        this.passwordHasher = passwordHasher;
        this.accountNoGenerator = accountNoGenerator;
    }

    @Transactional
    public RegisterAccountResult register(
            RegisterAccountCommand command) {
        /*1. 先统一处理输入*/
        String username = command.username().trim();
        String phone = trimToNull(command.phone());
        String email = trimToNull(command.email());
        String nickname = trimToNull(command.nickname());

        /*2. 领域规则：* 用户名 / 手机号 / 邮箱不能重复*/
        registrationPolicy.check(username, phone, email);

        /* 3. 领域规则：* 检查密码是否合法*/
        passwordPolicy.check(command.password());

        /* 4. 技术能力：* 对密码进行哈希*/
        String passwordHash = passwordHasher.hash(command.password());

        /*5. 生成账号编号*/
        String accountNo = accountNoGenerator.next();

        /** 6. 调用领域模型创建账号*/
        Account account = Account.registerNormalUser(accountNo, username, phone, email, passwordHash, nickname);

        /*7. 保存聚合*/
        accountRepository.save(account);

        /*8. 返回结果*/
        return new RegisterAccountResult(account.getId(), account.getAccountNo(), account.getUsername());
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {return null;}
        return value.trim();
    }
}
