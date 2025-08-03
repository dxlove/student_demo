package com.demo.studentdemo.exception;

import com.demo.studentdemo.vo.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleBusinessException(BusinessException ex) {
        return ApiResponse.error(
                ex.getCode(),
                ex.getMessage() != null ? ex.getMessage() : "Business error"
        );
    }

    // 处理系统异常
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Object> handleException(Exception ex) {
        // 生产环境隐藏详细错误
        return ApiResponse.error(
                500,
                "Internal server error"
        );
    }
}

