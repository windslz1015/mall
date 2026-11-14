package com.mall.iam.infrastructure.persistence.account.repository;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mall.iam.application.account.port.persistence.LoginAccountStore;
import com.mall.iam.application.account.query.LoginAccount;
import com.mall.iam.domain.account.model.AccountType;
import com.mall.iam.domain.account.model.AccountStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import com.mall.iam.domain.account.model.Account;
import com.mall.iam.domain.account.repository.AccountRepository;
import com.mall.iam.infrastructure.persistence.account.mapper.AccountMapper;
import com.mall.iam.infrastructure.persistence.account.mapper.AccountProfileMapper;
import com.mall.iam.infrastructure.persistence.account.po.IamAccountPO;
import com.mall.iam.infrastructure.persistence.account.po.IamAccountProfilePO;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepositoryImpl implements AccountRepository, LoginAccountStore {
    private final AccountMapper accountMapper;
    private final AccountProfileMapper profileMapper;

    public AccountRepositoryImpl(AccountMapper accountMapper, AccountProfileMapper profileMapper) {
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
        if (accountMapper.insert(accountPO) != 1) {
            throw new IllegalStateException("保存账号失败");
        }
        Long accountId = accountPO.getId();
        account.assignId(accountId);
        IamAccountProfilePO profilePO = new IamAccountProfilePO();
        profilePO.setAccountId(accountId);
        profilePO.setNickname(account.getProfile().getNickname());
        profilePO.setAvatarUrl(account.getProfile().getAvatarUrl());
        profilePO.setRealName(account.getProfile().getRealName());
        if (profileMapper.insert(profilePO) != 1) {
            throw new IllegalStateException("保存账号资料失败");
        }
    }


    @Override
    public Optional<LoginAccount> findByUsernameForUpdate(String username) {
        return findOne(new LambdaQueryWrapper<IamAccountPO>()
                .eq(IamAccountPO::getUsername, username).last("FOR UPDATE"));
    }

    @Override
    public Optional<LoginAccount> findByPhoneForUpdate(String phone) {
        return findOne(new LambdaQueryWrapper<IamAccountPO>()
                .eq(IamAccountPO::getPhone, phone).last("FOR UPDATE"));
    }

    @Override
    public Optional<LoginAccount> findById(Long accountId) {
        return Optional.ofNullable(accountMapper.selectById(accountId)).map(this::convertLoginAccount);
    }

    private Optional<LoginAccount> findOne(LambdaQueryWrapper<IamAccountPO> wrapper) {
        return Optional.ofNullable(accountMapper.selectOne(wrapper)).map(this::convertLoginAccount);
    }

    private LoginAccount convertLoginAccount(IamAccountPO po) {
        AccountType type = null;
        for (AccountType candidate : AccountType.values()) {
            if (po.getAccountType() != null && candidate.getCode() == po.getAccountType()) {
                type = candidate;
                break;
            }
        }
        AccountStatus status = null;
        for (AccountStatus candidate : AccountStatus.values()) {
            if (po.getStatus() != null && candidate.getCode() == po.getStatus()) {
                status = candidate;
                break;
            }
        }
        if (type == null || status == null || po.getFailedLoginCount() == null) {
            throw new IllegalStateException("iam_account contains invalid type/status/login counter");
        }
        return new LoginAccount(po.getId(), po.getAccountNo(), po.getUsername(), type, status,
                po.getPasswordHash(), po.getFailedLoginCount(), po.getLockedUntil());
    }

    @Override
    public void updateFailureState(Long accountId, int failures, LocalDateTime lockedUntil) {
        var update = new LambdaUpdateWrapper<IamAccountPO>()
                .eq(IamAccountPO::getId, accountId)
                .set(IamAccountPO::getFailedLoginCount, failures)
                .set(IamAccountPO::getLockedUntil, lockedUntil);
        if (accountMapper.update(null, update) != 1) {
            throw new IllegalStateException("update login failures failed");
        }
    }

    @Override
    public void updateSuccessfulLogin(Long accountId, String clientIp, LocalDateTime nowUtc) {
        var update = new LambdaUpdateWrapper<IamAccountPO>()
                .eq(IamAccountPO::getId, accountId)
                .set(IamAccountPO::getLastLoginAt, nowUtc)
                .set(IamAccountPO::getLastLoginIp, clientIp);
        if (accountMapper.update(null, update) != 1) {
            throw new IllegalStateException("update last login failed");
        }
    }

}
