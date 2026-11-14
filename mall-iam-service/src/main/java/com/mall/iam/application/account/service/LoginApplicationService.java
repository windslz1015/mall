package com.mall.iam.application.account.service;

import com.mall.common.exception.BusinessException;
import com.mall.iam.application.account.command.NormalUserLoginCommand;
import com.mall.iam.application.account.command.VerifyLoginAccountCommand;
import com.mall.iam.application.account.port.audit.LoginAuditPort;
import com.mall.iam.application.account.port.authorization.LoginAuthorizationPort;
import com.mall.iam.application.account.port.persistence.LoginAccountStore;
import com.mall.iam.application.account.port.risk.LoginRiskPort;
import com.mall.iam.application.account.port.security.TokenProvider;
import com.mall.iam.application.account.port.session.LoginSessionStore;
import com.mall.iam.application.account.query.GrantedLoginAuthorization;
import com.mall.iam.application.account.result.IssuedLoginTokens;
import com.mall.iam.application.account.result.NormalUserLoginResult;
import com.mall.iam.application.account.result.VerifiedLoginAccountResult;
import com.mall.iam.domain.account.error.AccountErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class LoginApplicationService {
    private final LoginAccountVerificationService verifier;
    private final LoginAccountStore accounts;
    private final LoginRiskPort risk;
    private final LoginAuthorizationPort authorization;
    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokens;
    private final LoginSessionStore sessions;
    private final LoginAuditPort audit;
    private final long refreshTtlSeconds;

    public LoginApplicationService(LoginAccountVerificationService verifier, LoginAccountStore accounts,
            LoginRiskPort risk, LoginAuthorizationPort authorization, TokenProvider tokenProvider,
            RefreshTokenService refreshTokens, LoginSessionStore sessions, LoginAuditPort audit,
            @Value("${iam.jwt.refresh-ttl-seconds:604800}") long refreshTtlSeconds) {
        if (refreshTtlSeconds <= 0) throw new IllegalArgumentException("invalid refresh token TTL");
        this.verifier = verifier;
        this.accounts = accounts;
        this.risk = risk;
        this.authorization = authorization;
        this.tokenProvider = tokenProvider;
        this.refreshTokens = refreshTokens;
        this.sessions = sessions;
        this.audit = audit;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    public IssuedLoginTokens login(NormalUserLoginCommand command) {
        String identifier = command.loginIdentifier() == null ? "" : command.loginIdentifier().trim();
        try {
            risk.beforeLogin(identifier);
            VerifiedLoginAccountResult verified = verifier.verify(
                    new VerifyLoginAccountCommand(identifier, command.password()));
            GrantedLoginAuthorization grants = authorization.loadNormalUserAuthorization(verified.accountId());
            if (!grants.roles().contains("NORMAL_USER")) {
                throw new BusinessException(AccountErrorCode.LOGIN_ACCOUNT_UNAVAILABLE);
            }
            // Clear identifier risk after successfully authenticating, before creating the session.
            risk.clearFailures(identifier);
            String sid = UUID.randomUUID().toString();
            String opaqueRefresh = refreshTokens.generate(sid);
            String jwt = tokenProvider.issueAccessToken(verified.accountId(), sid, grants);
            sessions.create(sid, verified.accountId(), refreshTokens.hash(opaqueRefresh), refreshTtlSeconds);
            try {
                accounts.updateSuccessfulLogin(verified.accountId(), command.clientIp(),
                        LocalDateTime.now(ZoneOffset.UTC));
                audit.record(verified.accountId(), identifier, command.clientIp(), command.userAgent(), true, null);
            } catch (RuntimeException e) {
                try { sessions.revokeSession(sid); } catch (RuntimeException suppressed) { e.addSuppressed(suppressed); }
                throw e;
            }
            return new IssuedLoginTokens(new NormalUserLoginResult(verified.accountId(),
                    verified.accountNo(), verified.username(), grants.roles(), jwt, "Bearer",
                    tokenProvider.accessTokenSeconds()), opaqueRefresh);
        } catch (BusinessException e) {
            try {
                if (e.getCode() == AccountErrorCode.LOGIN_BAD_CREDENTIALS.getCode()) {
                    risk.recordBadCredentials(identifier);
                }
            } finally {
                audit.record(null, identifier, command.clientIp(), command.userAgent(), false,
                        "E" + e.getCode());
            }
            throw e;
        }
    }
}
