package com.mall.reliability.domain.reconciliation.model;

/** 对账差异严重度；与 reliability_reconciliation_difference.severity 对齐。 */
public enum DiscrepancySeverity {
    LOW(1),
    NORMAL(2),
    HIGH(3),
    CRITICAL(4);

    private final int code;
    DiscrepancySeverity(int code) { this.code = code; }
    public int getCode() { return code; }
}
