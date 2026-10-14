package com.mall.common.api;

/**
 * 业务错误码接口。code描述类型，message描述具体信息。
 * 为什么叫做业务错误码接口：因为成功只有一种，错误有很多种。
 */
public interface ErrorCode {

    int getCode();

    String getMessage();
}
