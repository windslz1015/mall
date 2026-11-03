package com.mall.reliability.domain.reconciliation.model;

/** 对账批次状态；与 reliability_reconciliation_batch.status 对齐。 */
public enum ReconciliationBatchStatus {
    PENDING(0),
    RUNNING(10),
    COMPLETED(20),
    FAILED(30),
    CANCELLED(40);

    private final int code;
    ReconciliationBatchStatus(int code) { this.code = code; }
    public int getCode() { return code; }
}
