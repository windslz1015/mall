package com.mall.iam.interfaces.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "登录账号不能为空")
        @Size(max = 64, message = "登录账号长度不能超过64位")
        String loginIdentifier,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度必须为8-64位")
        String password
) {}
