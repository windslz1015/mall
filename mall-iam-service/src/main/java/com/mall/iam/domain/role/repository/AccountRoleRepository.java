
package com.mall.iam.domain.role.repository;

public interface AccountRoleRepository {

    Long findEnabledRoleIdByCode(String roleCode);

    void assignRole(Long accountId, Long roleId);
}