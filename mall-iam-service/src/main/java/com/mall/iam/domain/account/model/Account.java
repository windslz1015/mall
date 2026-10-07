package com.mall.iam.domain.account.model;

public class Account {

    private Long id;

    private String accountNo;

    private AccountType accountType;

    private String username;

    private String phone;

    private String email;

    private String passwordHash;

    private AccountStatus status;

    private AccountProfile profile;

    private Account() {
    }

    /**
     * 普通用户注册
     */
    public static Account registerNormalUser(
            String accountNo,
            String username,
            String phone,
            String email,
            String passwordHash,
            String nickname) {

        if (accountNo == null || accountNo.isBlank()) {
            throw new IllegalArgumentException("账号编号不能为空");
        }

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        username = username.trim();

        if (username.length() < 4 || username.length() > 64) {
            throw new IllegalArgumentException(
                    "用户名长度必须为4-64位"
            );
        }

        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        Account account = new Account();

        account.accountNo = accountNo;

        /*
         * 业务规则：
         * 用户自主注册只能创建普通用户。
         */
        account.accountType = AccountType.NORMAL_USER;

        account.username = username;

        account.phone = normalize(phone);

        account.email = normalize(email);

        account.passwordHash = passwordHash;

        /*
         * 业务规则：
         * 新注册账号默认处于正常状态。
         */
        account.status = AccountStatus.NORMAL;

        account.profile = AccountProfile.create(
                nickname == null || nickname.isBlank()
                        ? username
                        : nickname
        );

        return account;
    }

    private static String normalize(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    public Long getId() {
        return id;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public String getUsername() {
        return username;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public AccountProfile getProfile() {
        return profile;
    }

    public void assignId(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("账号ID非法");
        }

        this.id = id;
    }

}
