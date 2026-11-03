package com.mall.order.domain.operation.model;

/**
 * 关键操作处理状态：处理中、成功、失败；重试应校验请求指纹。
 * code 必须与当前 Flyway SQL 状态 CHECK 约束保持一致。
 */
public enum OrderOperationStatus {
    PROCESSING(1),
SUCCEEDED(2),
FAILED(3);

    private final int code;

    OrderOperationStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
