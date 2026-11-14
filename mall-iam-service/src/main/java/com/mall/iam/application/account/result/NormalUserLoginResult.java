package com.mall.iam.application.account.result;
import java.util.List;

public record NormalUserLoginResult(Long accountId, String accountNo, String username,
        List<String> roles, String accessToken, String tokenType, long expiresIn) {}
