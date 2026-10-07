package com.mall.iam.domain.account.service;

import com.mall.common.exception.BusinessException;
import com.mall.iam.domain.account.error.AccountErrorCode;


public class PasswordPolicy {

    public void check(String password) {

        if (password == null || password.isBlank()) {
            throw new BusinessException(AccountErrorCode.PASSWORD_EMPTY);
        }

        if (password.length() < 8 || password.length() > 64) {
            throw new BusinessException(AccountErrorCode.PASSWORD_LENGTH_INVALID);
        }
    }
}
