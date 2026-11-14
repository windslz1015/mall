package com.mall.iam.application.account.port.session;

import com.mall.iam.application.account.query.LoginSession;
import java.util.Optional;
public interface LoginSessionStore {
    void create(String sessionId, Long accountId, String refreshHash, long ttlSeconds);
    Optional<LoginSession> findActive(String sessionId);
    boolean rotate(String sessionId, Long accountId, String existingHash, String nextHash, long ttlSeconds);
    void revokeIfMatches(String sessionId, String refreshHash);
    void revokeSession(String sessionId);
}
