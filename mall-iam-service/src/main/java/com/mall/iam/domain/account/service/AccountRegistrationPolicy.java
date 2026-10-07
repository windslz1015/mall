package com.mall.iam.domain.account.service;

import com.mall.common.exception.BusinessException;
import com.mall.iam.domain.account.error.AccountErrorCode;
import com.mall.iam.domain.account.repository.AccountRepository;

public class AccountRegistrationPolicy {

    private final AccountRepository accountRepository;

    public AccountRegistrationPolicy(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public void check(String username, String phone, String email) {
        if (accountRepository.existsByUsername(username)) {
            throw new BusinessException(AccountErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (phone != null && !phone.isBlank() && accountRepository.existsByPhone(phone)) {
            throw new BusinessException(AccountErrorCode.PHONE_ALREADY_EXISTS);
        }
        if (email != null && !email.isBlank() && accountRepository.existsByEmail(email)) {
            throw new BusinessException(AccountErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }
}
