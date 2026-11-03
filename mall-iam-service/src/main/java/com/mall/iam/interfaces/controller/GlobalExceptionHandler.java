package com.mall.iam.interfaces.controller;

import com.mall.common.api.CommonCode;
import com.mall.common.api.Result;
import com.mall.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/*
目前异常分类：参数异常（JSON解析，DTO校验），业务异常，兜底异常。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> invalidBody(
            HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Result.failure(CommonCode.BAD_REQUEST.getCode(), "请求体格式不正确"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream().findFirst()
                .map(error -> error.getDefaultMessage()).orElse("请求参数不合法");
        return ResponseEntity.badRequest().body(Result.failure(CommonCode.BAD_REQUEST.getCode(), message));
    }


    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> business(BusinessException e) {
        HttpStatus status = switch (e.getCode()) {
            case 10001, 10002, 10003, 10009 -> HttpStatus.CONFLICT;
            case 10008, 10011, 10012 -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(Result.failure(e.getCode(), e.getMessage()));
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> unexpected(Exception e) {
        log.error("IAM服务发生未预期异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.failure(CommonCode.INTERNAL_ERROR));
    }


}
