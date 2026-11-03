package com.mall.product.domain.product.model;

/**
 * SKU 在当前商品下是否可供选择；不等于库存是否充足。
 * 数值与 Flyway 迁移的 CHECK 约束保持一致。
 */
public enum SkuStatus {
    ACTIVE(1),
    INACTIVE(2);

    private final int code;

    SkuStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
