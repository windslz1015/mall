package com.mall.iam.infrastructure.persistence.account.repository;

import com.mall.iam.application.account.port.audit.LoginAuditPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.regex.Pattern;

@Repository
public class JdbcLoginAuditRepository implements LoginAuditPort {
    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private final JdbcTemplate jdbc;
    public JdbcLoginAuditRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long accountId, String identifier, String clientIp, String userAgent,
                       boolean success, String failureReason) {
        String safeIdentifier = identifier == null ? null : identifier.substring(0, Math.min(128, identifier.length()));
        // Avoid exposing full mobile numbers in audit tables.
        if (safeIdentifier != null && PHONE.matcher(safeIdentifier).matches()) {
            safeIdentifier = safeIdentifier.substring(0, 3) + "****" + safeIdentifier.substring(7);
        }
        String ua = userAgent == null ? null : userAgent.substring(0, Math.min(512, userAgent.length()));
        String ip = clientIp == null ? null : clientIp.substring(0, Math.min(45, clientIp.length()));
        jdbc.update("""
                INSERT INTO iam_login_log
                (account_id, login_identifier, login_type, login_result, failure_reason, login_ip, user_agent, login_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(3))
                """, accountId, safeIdentifier, identifier != null && PHONE.matcher(identifier).matches() ? 2 : 1,
                success ? 1 : 2, failureReason, ip, ua);
    }
}
