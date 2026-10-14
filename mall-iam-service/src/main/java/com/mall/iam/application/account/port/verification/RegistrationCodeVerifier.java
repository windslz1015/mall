
package com.mall.iam.application.account.port.verification;

/**
 * 注册验证码校验端口
 *
 * Application 层只依赖该接口，
 * 不直接依赖 Redis。
 */
public interface RegistrationCodeVerifier {

    /**
     * 校验并消费注册验证码
     *
     * @param phone 手机号
     * @param verificationCode 用户输入的验证码
     *
     * 校验成功：正常返回
     * 校验失败：抛出异常
     */
    void verify(String phone, String verificationCode);
}