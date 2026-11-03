package com.mall.iam.application.account.port.verification;

/** Redis 验证码以短租约锁定，MySQL 提交后才永久消费。 */
public interface RegistrationCodeVerifier {
    /** 检查验证码并获得一次性租约。成功返回随机租约 token。 */
    String reserve(String phone, String code);
    /** MySQL 已经提交：原子确认消费。此操作应尽力完成，允许重试。 */
    void confirm(String phone, String token, String code);
    /** MySQL 回滚：仅释放属于当前 token 的租约，不删除验证码。 */
    void release(String phone, String token);
}
