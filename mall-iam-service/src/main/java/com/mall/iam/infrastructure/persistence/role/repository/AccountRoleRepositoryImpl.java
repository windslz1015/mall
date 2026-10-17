package com.mall.iam.infrastructure.persistence.role.repository;


import com.mall.iam.domain.role.repository.AccountRoleRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRoleRepositoryImpl
        implements AccountRoleRepository {

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

        if (rows != 1) {
            throw new IllegalStateException("账号角色关系保存失败");
        }
    }
}