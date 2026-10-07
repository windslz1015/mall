package com.mall.iam.interfaces.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "用户名不能为空")
        @Size(
                min = 4,
                max = 64,
                message = "用户名长度必须为4-64位"
        )
        String username,

        @NotBlank(message = "密码不能为空")
        String password,

        @Size(
                max = 20,
                message = "手机号长度不能超过20位"
        )
        String phone,

        @Email(message = "邮箱格式不正确")
        @Size(
                max = 128,
                message = "邮箱长度不能超过128位"
        )
        String email,

        @Size(
                max = 64,
                message = "昵称长度不能超过64位"
        )
        String nickname

) {
}
