package com.mall.iam.interfaces.controller;

import com.mall.common.api.CommonCode;
import com.mall.common.api.Result;
import com.mall.common.exception.BusinessException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {


    /***非法参数异常*/
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        return Result.failure(CommonCode.BAD_REQUEST.getCode(), e.getMessage());
    }


    /***参数校验异常*/
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("请求参数不合法");
        return Result.failure(CommonCode.BAD_REQUEST.getCode(), message);
    }


    /***业务规则异常*/
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.failure(e.getCode(), e.getMessage());
    }


    /***系统异常*/
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {

        return Result.failure(CommonCode.INTERNAL_ERROR);
    }
}
