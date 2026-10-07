package com.mall.common.exception;

import com.mall.common.api.ErrorCode;

/**
 * 可预期的业务异常。
 * 作用1：异常包装，能对外提供业务化描述，又不会丢失底层异常信息。
 *
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 50000; // 通用业务异常码
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 50000;
    }


    public int getCode() {
        return code;
    }
}
