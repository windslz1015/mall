
package com.mall.iam.domain.role.service;

import com.mall.iam.domain.account.model.AccountType;

public class DefaultRolePolicy {

    public String defaultRoleCode(
            AccountType accountType) {

        if (accountType == AccountType.NORMAL_USER) {
            return "NORMAL_USER";
        }

        throw new IllegalArgumentException(
                "不支持的账号类型"
        );
    }
}