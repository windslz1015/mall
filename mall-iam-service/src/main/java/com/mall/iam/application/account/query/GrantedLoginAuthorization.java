package com.mall.iam.application.account.query;
import java.util.List;
public record GrantedLoginAuthorization(List<String> roles, List<String> permissions) {}
