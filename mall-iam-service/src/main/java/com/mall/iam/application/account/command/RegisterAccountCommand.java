package com.mall.iam.application.account.command;

public record RegisterAccountCommand(

        String username,

        String password,

        String phone,

        String email,

        String nickname

) {
}
