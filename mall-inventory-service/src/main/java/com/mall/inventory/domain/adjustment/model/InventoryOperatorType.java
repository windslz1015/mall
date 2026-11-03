package com.mall.inventory.domain.adjustment.model;

/**
 * 人工/系统库存调整的操作人类型；账号数据由 IAM 独立管理。
 */
public enum InventoryOperatorType {
    MERCHANT(1),
    ADMIN(2),
    SYSTEM(3);

    private final int code;

    InventoryOperatorType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
