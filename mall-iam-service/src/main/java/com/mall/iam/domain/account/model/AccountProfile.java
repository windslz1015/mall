package com.mall.iam.domain.account.model;


public class AccountProfile {

    private String nickname;
    private String avatarUrl;
    private String realName;

    private AccountProfile() {
    }

    public static AccountProfile create(String nickname) {

        AccountProfile profile = new AccountProfile();

        if (nickname != null) {
            nickname = nickname.trim();

            if (nickname.length() > 64) {
                throw new IllegalArgumentException(
                        "昵称长度不能超过64个字符"
                );
            }
        }

        profile.nickname = nickname;

        return profile;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getRealName() {
        return realName;
    }

}
