package com.mall.product.domain.moderation.model;

/**
 * 平台商品监管动作；当前业务范围只有强制下架。
 * 数值与 Flyway 迁移的 CHECK 约束保持一致。
 */
public enum ModerationActionType {
    FORCE_OFF_SALE(1);

    private final int code;

    ModerationActionType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
