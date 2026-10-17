package com.mall.iam.infrastructure.authorization;

import com.mall.iam.application.account.port.authorization.AccountRoleAssignment;
import com.mall.iam.domain.account.model.AccountType;
import com.mall.iam.domain.role.repository.AccountRoleRepository;
import com.mall.iam.domain.role.service.DefaultRolePolicy;
import org.springframework.stereotype.Component;

@Component
public class DefaultAccountRoleAssignment
        implements AccountRoleAssignment {

    private final AccountRoleRepository accountRoleRepository;
    private final DefaultRolePolicy defaultRolePolicy;

    public DefaultAccountRoleAssignment(
            AccountRoleRepository accountRoleRepository,
            DefaultRolePolicy defaultRolePolicy) {

        this.accountRoleRepository = accountRoleRepository;
        this.defaultRolePolicy = defaultRolePolicy;
    }

    @Override
    public void assignDefaultRole(Long accountId) {

        // 1. 当前公开注册只能创建普通用户
        String roleCode = defaultRolePolicy.defaultRoleCode(
                AccountType.NORMAL_USER
        );

        // 2. 根据角色编码查询启用的角色
        Long roleId =
                accountRoleRepository.findEnabledRoleIdByCode(
                        roleCode
                );

        if (roleId == null) {
            throw new IllegalStateException(
                    "默认普通用户角色不存在或已禁用"
            );
        }

        // 3. 保存账号与角色关系
        accountRoleRepository.assignRole(accountId, roleId);
    }
}