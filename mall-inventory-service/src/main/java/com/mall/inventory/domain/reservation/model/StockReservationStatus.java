package com.mall.inventory.domain.reservation.model;

/**
 * 库存预占状态，与 inventory_reservation.status 的 CHECK 约束一致。
 */
public enum StockReservationStatus {
    LOCKED(1),
    CONFIRMED(2),
    RELEASED(3);

    private final int code;

    StockReservationStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
