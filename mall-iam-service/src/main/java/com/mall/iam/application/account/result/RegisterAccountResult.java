package com.mall.iam.application.account.result;

public record RegisterAccountResult(

        Long accountId,

        String accountNo,

        String username

) {
}