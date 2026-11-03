-- 购物车聚合根，一位普通用户（IAM account_id）仅有一个长期购物车。
-- account_id 为跨服务逻辑引用，不建立到 IAM 数据库的外键。
-- 购物车无需存订单状态或支付状态；只有 cart-service 可以修改本表。
CREATE TABLE cart (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '购物车主键ID',
    account_id BIGINT UNSIGNED NOT NULL COMMENT '普通用户账号ID（iam-service，逻辑引用）',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '购物车聚合乐观锁版本（业务写入时递增）',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='用户购物车（一位普通用户一个购物车）';
