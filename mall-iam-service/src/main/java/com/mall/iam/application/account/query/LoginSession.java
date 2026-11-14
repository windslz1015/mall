package com.mall.iam.application.account.query;
public record LoginSession(String sessionId, Long accountId, String refreshTokenHash) {}
