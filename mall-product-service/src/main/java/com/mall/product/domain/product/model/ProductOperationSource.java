package com.mall.product.domain.product.model;

/**
 * 商品上下架操作来源。
 * 数值与 Flyway 迁移的 CHECK 约束保持一致。
 */
public enum ProductOperationSource {
    MERCHANT(1),
    ADMIN(2),
    SYSTEM(3);

    private final int code;

    ProductOperationSource(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
