package com.mall.iam.infrastructure.persistence.account.repository;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.iam.domain.account.model.Account;
import com.mall.iam.domain.account.repository.AccountRepository;
import com.mall.iam.infrastructure.persistence.account.mapper.IamAccountMapper;
import com.mall.iam.infrastructure.persistence.account.mapper.IamAccountProfileMapper;
import com.mall.iam.infrastructure.persistence.account.po.IamAccountPO;
import com.mall.iam.infrastructure.persistence.account.po.IamAccountProfilePO;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepositoryImpl implements AccountRepository {
    private final IamAccountMapper accountMapper;
    private final IamAccountProfileMapper profileMapper;

    public AccountRepositoryImpl(IamAccountMapper accountMapper, IamAccountProfileMapper profileMapper) {
        this.accountMapper = accountMapper;
        this.profileMapper = profileMapper;
    }

    @Override
    public boolean existsByUsername(String username) {
        Long count = accountMapper.selectCount(
                new LambdaQueryWrapper<IamAccountPO>().eq(IamAccountPO::getUsername, username)
        );
        return count > 0;
    }
    @Override
    public boolean existsByPhone(String phone) {
        Long count = accountMapper.selectCount(
                new LambdaQueryWrapper<IamAccountPO>().eq(IamAccountPO::getPhone, phone)
        );
        return count > 0;
    }
    @Override
    public boolean existsByEmail(String email) {
        Long count = accountMapper.selectCount(
                new LambdaQueryWrapper<IamAccountPO>().eq(IamAccountPO::getEmail, email));
        return count > 0;
    }

    @Override
    public void save(Account account) {
        IamAccountPO accountPO = new IamAccountPO();
        accountPO.setAccountNo(account.getAccountNo());
        accountPO.setAccountType(account.getAccountType().getCode());
        accountPO.setUsername(account.getUsername());
        accountPO.setPhone(account.getPhone());
        accountPO.setEmail(account.getEmail());
        accountPO.setPasswordHash(account.getPasswordHash());
        accountPO.setStatus(account.getStatus().getCode());
        accountMapper.insert(accountPO);
        Long accountId = accountPO.getId();
        account.assignId(accountId);
        IamAccountProfilePO profilePO = new IamAccountProfilePO();
        profilePO.setAccountId(accountId);
        profilePO.setNickname(account.getProfile().getNickname());
        profilePO.setAvatarUrl(account.getProfile().getAvatarUrl());
        profilePO.setRealName(account.getProfile().getRealName());
        profileMapper.insert(profilePO);
    }
}
