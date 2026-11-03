package com.mall.product.domain.product.model;

/**
 * 商品 SPU 生命周期状态。
 * 数值与 Flyway 迁移的 CHECK 约束保持一致。
 */
public enum ProductStatus {
    DRAFT(0),
    ON_SALE(1),
    OFF_SALE(2),
    FORCED_OFF_SALE(3);

    private final int code;

    ProductStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
