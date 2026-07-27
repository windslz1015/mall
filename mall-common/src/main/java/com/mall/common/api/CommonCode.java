package com.mall.common.api;

/**
 * 通用错误码。
 */
public enum CommonCode implements ErrorCode {

    SUCCESS(0, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "请求资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误");

    private final int code;
    private final String message;

    CommonCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
