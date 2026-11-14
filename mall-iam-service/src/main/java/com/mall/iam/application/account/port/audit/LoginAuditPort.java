package com.mall.iam.application.account.port.audit;

public interface LoginAuditPort {
    void record(Long accountId, String identifier, String clientIp, String userAgent,
                boolean success, String failureReason);
}
