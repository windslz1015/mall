package com.mall.inventory.domain.operation.model;

/**
 * 每次库存原子变更的类型，与 inventory_operation.operation_type 一致。
 */
public enum InventoryOperationType {
    ADJUST(1),
    RESERVE(2),
    CONFIRM(3),
    RELEASE(4);

    private final int code;

    InventoryOperationType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
