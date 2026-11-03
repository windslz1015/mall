package com.mall.iam.domain.account.service;

import com.mall.common.exception.BusinessException;
import com.mall.iam.domain.account.error.AccountErrorCode;
import java.nio.charset.StandardCharsets;


public class PasswordPolicy {

    public void check(String password) {

        if (password == null || password.isBlank()) {
            throw new BusinessException(AccountErrorCode.PASSWORD_EMPTY);
        }

        if (password.length() < 8 || password.length() > 64) {
            throw new BusinessException(AccountErrorCode.PASSWORD_LENGTH_INVALID);
        }
        // BCrypt 仅处理前 72 字节，避免不同超长密码产生相同散列匹配效果。
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(AccountErrorCode.PASSWORD_BYTES_EXCEEDED);
        }
    }
}
