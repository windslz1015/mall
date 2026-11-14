package com.mall.iam.application.account.service;

import com.mall.common.exception.BusinessException;
import com.mall.iam.application.account.port.authorization.LoginAuthorizationPort;
import com.mall.iam.application.account.port.persistence.LoginAccountStore;
import com.mall.iam.application.account.port.security.TokenProvider;
import com.mall.iam.application.account.port.session.LoginSessionStore;
import com.mall.iam.application.account.query.GrantedLoginAuthorization;
import com.mall.iam.application.account.query.LoginAccount;
import com.mall.iam.application.account.query.LoginSession;
import com.mall.iam.application.account.result.IssuedLoginTokens;
import com.mall.iam.application.account.result.NormalUserLoginResult;
import com.mall.iam.domain.account.error.AccountErrorCode;
import com.mall.iam.domain.account.model.AccountStatus;
import com.mall.iam.domain.account.model.AccountType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class RefreshLoginApplicationService {
    private final RefreshTokenService tokens;
    private final LoginSessionStore sessions;
    private final LoginAccountStore accounts;
    private final LoginAuthorizationPort authorization;
    private final TokenProvider tokenProvider;
    private final long refreshTtlSeconds;

    public RefreshLoginApplicationService(RefreshTokenService tokens, LoginSessionStore sessions,
            LoginAccountStore accounts, LoginAuthorizationPort authorization, TokenProvider tokenProvider,
            @Value("${iam.jwt.refresh-ttl-seconds:604800}") long refreshTtlSeconds) {
        this.tokens = tokens;
        this.sessions = sessions;
        this.accounts = accounts;
        this.authorization = authorization;
        this.tokenProvider = tokenProvider;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    public IssuedLoginTokens refresh(String rawRefresh) {
        String sid = tokens.sessionId(rawRefresh);
        LoginSession session = sessions.findActive(sid).orElseThrow(this::invalid);
        String previousHash = tokens.hash(rawRefresh);
        if (!MessageDigest.isEqual(previousHash.getBytes(StandardCharsets.US_ASCII),
                session.refreshTokenHash().getBytes(StandardCharsets.US_ASCII))) {
            throw invalid();
        }
        LoginAccount account = accounts.findById(session.accountId()).orElseThrow(this::invalid);
        if (account.accountType() != AccountType.NORMAL_USER
                || account.status() != AccountStatus.NORMAL
                || account.isTemporarilyLockedAt(LocalDateTime.now(ZoneOffset.UTC))) {
            sessions.revokeIfMatches(sid, previousHash);
            throw invalid();
        }
        GrantedLoginAuthorization grants = authorization.loadNormalUserAuthorization(account.accountId());
        if (!grants.roles().contains("NORMAL_USER")) {
            sessions.revokeIfMatches(sid, previousHash);
            throw invalid();
        }
        String newRefresh = tokens.generate(sid);
        String accessToken = tokenProvider.issueAccessToken(account.accountId(), sid, grants);
        // Atomic compare-and-swap: the same Refresh Token cannot be used twice.
        if (!sessions.rotate(sid, account.accountId(), previousHash, tokens.hash(newRefresh), refreshTtlSeconds)) {
            throw invalid();
        }
        return new IssuedLoginTokens(new NormalUserLoginResult(account.accountId(), account.accountNo(),
                account.username(), grants.roles(), accessToken, "Bearer", tokenProvider.accessTokenSeconds()),
                newRefresh);
    }

    public void logout(String rawRefresh) {
        if (rawRefresh == null || rawRefresh.isBlank()) return;
        try {
            String sid = tokens.sessionId(rawRefresh);
            sessions.revokeIfMatches(sid, tokens.hash(rawRefresh));
        } catch (BusinessException ignored) {
            // Logout is idempotent and must not disclose whether the cookie was valid.
        }
    }

    private BusinessException invalid() { return new BusinessException(AccountErrorCode.LOGIN_REFRESH_INVALID); }
}
