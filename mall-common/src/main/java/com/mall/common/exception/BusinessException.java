package com.mall.common.exception;

import com.mall.common.api.ErrorCode;

/**
 * 业务异常，可预期的业务中的异常，用于表达业务规则不满足、业务操作无法继续等情况。
 * 业务异常性质:可能非常高（用户正常操作就会触发），所以默认不记录日志。
 * 作用1：异常包装，能对外提供业务化描述，又不会丢失底层异常信息。
 * 关于异常：处理异常情况。
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

    public int getCode() {
        return code;
    }
}
