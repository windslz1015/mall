package com.mall.admin.domain.casefile.model;

/** 监管案件和异常排查优先级；与数据库约束一致。 */
public enum RegulationPriority {
    LOW(1),
    NORMAL(2),
    HIGH(3),
    CRITICAL(4);

    private final int code;
    RegulationPriority(int code) { this.code = code; }
    public int getCode() { return code; }
}
