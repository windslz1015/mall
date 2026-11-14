package com.mall.iam.application.account.command;
public record NormalUserLoginCommand(String loginIdentifier, String password,
                                     String clientIp, String userAgent) {}
