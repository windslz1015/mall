package com.mall.admin.domain.investigation.model;

/** 异常排查记录状态；与 admin_exception_investigation.status 一致。 */
public enum InvestigationStatus {
    OPEN(0),
    INVESTIGATING(10),
    RESOLVED(20),
    CLOSED(30);

    private final int code;
    InvestigationStatus(int code) { this.code = code; }
    public int getCode() { return code; }
}
