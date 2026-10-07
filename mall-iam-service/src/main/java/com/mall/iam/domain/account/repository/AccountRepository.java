package com.mall.iam.domain.account.repository;


import com.mall.iam.domain.account.model.Account;

public interface AccountRepository {

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    void save(Account account);
}