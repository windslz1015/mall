package com.mall.product.domain.product.model;

/**
 * 商品图片用途，图片文件本身位于 OSS 等对象存储。
 * 数值与 Flyway 迁移的 CHECK 约束保持一致。
 */
public enum ProductImageType {
    COVER(1),
    GALLERY(2),
    SKU(3);

    private final int code;

    ProductImageType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
