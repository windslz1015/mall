package com.mall.iam.infrastructure.config;

import com.mall.iam.domain.account.repository.AccountRepository;
import com.mall.iam.domain.account.service.AccountRegistrationPolicy;
import com.mall.iam.domain.account.service.PasswordPolicy;
import com.mall.iam.domain.role.service.DefaultRolePolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainBeanConfig {

    @Bean
    public AccountRegistrationPolicy accountRegistrationPolicy(AccountRepository accountRepository) {
        return new AccountRegistrationPolicy(accountRepository);
    }
    @Bean
    public PasswordPolicy passwordPolicy() {
        return new PasswordPolicy();
    }
    @Bean
    public DefaultRolePolicy defaultRolePolicy() {
        return new DefaultRolePolicy();
    }

}
