package com.mall.iam.application.account.query;

import com.mall.iam.domain.account.model.AccountStatus;
import com.mall.iam.domain.account.model.AccountType;

import java.time.LocalDateTime;

/**
 * 登录凭证校验所需的账号快照，不包含个人资料，也不向客户端返回密码哈希。
 * lockedUntil 使用 UTC 对应数据库 DATETIME(3)。
 */
public record LoginAccount(
        Long accountId,
        String accountNo,
        String username,
        AccountType accountType,
        AccountStatus status,
        String passwordHash,
        int failedLoginCount,
        LocalDateTime lockedUntil
) {
    public boolean isTemporarilyLockedAt(LocalDateTime nowUtc) {
        return lockedUntil != null && lockedUntil.isAfter(nowUtc);
    }

    public boolean hasExpiredTemporaryLockAt(LocalDateTime nowUtc) {
        return lockedUntil != null && !lockedUntil.isAfter(nowUtc);
    }
}
