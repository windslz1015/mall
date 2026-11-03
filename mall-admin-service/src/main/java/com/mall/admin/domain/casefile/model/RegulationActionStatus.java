package com.mall.admin.domain.casefile.model;

/** 监管处置请求状态；实际资源状态由对应业务服务拥有。 */
public enum RegulationActionStatus {
    CREATED(0),
    REQUESTED(10),
    SUCCEEDED(20),
    FAILED(30),
    CANCELLED(40);

    private final int code;
    RegulationActionStatus(int code) { this.code = code; }
    public int getCode() { return code; }
}
