package com.mall.iam.infrastructure.persistence.role.repository;


import com.mall.iam.domain.role.repository.AccountRoleRepository;
import com.mall.iam.application.account.port.authorization.LoginAuthorizationPort;
import com.mall.iam.application.account.query.GrantedLoginAuthorization;
import java.util.List;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRoleRepositoryImpl
        implements AccountRoleRepository, LoginAuthorizationPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AccountRoleRepositoryImpl(
            NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long findEnabledRoleIdByCode(String roleCode) {

        String sql = """
                SELECT id
                FROM iam_role
                WHERE role_code = :roleCode
                  AND status = 1
                LIMIT 1
                """;

        var params = new MapSqlParameterSource()
                .addValue("roleCode", roleCode);

        var ids = jdbcTemplate.queryForList(
                sql, params, Long.class
        );

        return ids.isEmpty() ? null : ids.get(0);
    }

    @Override
    public void assignRole(Long accountId, Long roleId) {

        String sql = """
                INSERT INTO sys_account_role
                    (account_id, role_id)
                VALUES
                    (:accountId, :roleId)
                """;

        var params = new MapSqlParameterSource()
                .addValue("accountId", accountId)
                .addValue("roleId", roleId);

        int rows = jdbcTemplate.update(sql, params);

        if (rows != 1) {throw new IllegalStateException("账号角色关系保存失败");}
    }

    @Override
    public GrantedLoginAuthorization loadNormalUserAuthorization(Long accountId) {
        var args = new MapSqlParameterSource("accountId", accountId);
        List<String> roles = jdbcTemplate.queryForList("""
                SELECT DISTINCT r.role_code FROM sys_account_role ar
                JOIN iam_role r ON r.id = ar.role_id
                WHERE ar.account_id = :accountId AND r.status = 1
                  AND (r.account_type IS NULL OR r.account_type = 1)
                ORDER BY r.role_code
                """, args, String.class);
        // Only the ordinary-user role can authenticate at this endpoint.
        if (!roles.contains("NORMAL_USER")) return new GrantedLoginAuthorization(List.of(), List.of());
        List<String> permissions = jdbcTemplate.queryForList("""
                SELECT DISTINCT p.permission_code FROM sys_account_role ar
                JOIN iam_role r ON r.id = ar.role_id AND r.status = 1
                JOIN iam_role_permission rp ON rp.role_id = r.id
                JOIN iam_permission p ON p.id = rp.permission_id AND p.status = 1
                WHERE ar.account_id = :accountId
                  AND (r.account_type IS NULL OR r.account_type = 1)
                ORDER BY p.permission_code
                """, args, String.class);
        return new GrantedLoginAuthorization(List.copyOf(roles), List.copyOf(permissions));
    }

}
