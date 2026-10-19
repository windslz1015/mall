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

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream().findFirst()
                .map(error -> error.getDefaultMessage()).orElse("请求参数不合法");
        return ResponseEntity.badRequest().body(Result.failure(CommonCode.BAD_REQUEST.getCode(), message));
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Result<Void>> badRequest(Exception e) {
        return ResponseEntity.badRequest().body(Result.failure(CommonCode.BAD_REQUEST));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> business(BusinessException e) {
        HttpStatus status = switch (e.getCode()) {
            case 10001, 10002, 10003, 10009 -> HttpStatus.CONFLICT;
            case 10008 -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(Result.failure(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Result<Void>> database(DataAccessException e) {
        log.error("IAM 数据库异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.failure(CommonCode.INTERNAL_ERROR));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> unexpected(Exception e) {
        log.error("IAM 未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.failure(CommonCode.INTERNAL_ERROR));
    }
}
