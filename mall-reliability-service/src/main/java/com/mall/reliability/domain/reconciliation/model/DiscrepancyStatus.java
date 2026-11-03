package com.mall.reliability.domain.reconciliation.model;

/** 对账差异状态；与 reliability_reconciliation_difference.status 对齐。 */
public enum DiscrepancyStatus {
    OPEN(0),
    RECOVERY_PENDING(10),
    RESOLVED(20),
    IGNORED(30);

    private final int code;
    DiscrepancyStatus(int code) { this.code = code; }
    public int getCode() { return code; }
}
