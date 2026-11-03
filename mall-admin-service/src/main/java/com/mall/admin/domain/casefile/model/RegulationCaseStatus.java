package com.mall.admin.domain.casefile.model;

/** 监管案件状态；与 admin_regulation_case.status 一致。 */
public enum RegulationCaseStatus {
    OPEN(0),
    IN_REVIEW(10),
    ACTION_PENDING(20),
    RESOLVED(30),
    CLOSED(40),
    DISMISSED(50);

    private final int code;
    RegulationCaseStatus(int code) { this.code = code; }
    public int getCode() { return code; }
}
