package com.mall.admin.domain.audit.model;

/** 管理员操作处理结果；与 admin_operation_log.outcome 一致。 */
public enum AuditOutcome {
    STARTED(0),
    SUCCEEDED(10),
    FAILED(20);

    private final int code;
    AuditOutcome(int code) { this.code = code; }
    public int getCode() { return code; }
}
