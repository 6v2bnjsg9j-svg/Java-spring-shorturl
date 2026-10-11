package com.wcy.shorturl.common;

import lombok.Data;

/**
 * 统一响应体
 */
@Data
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.code = ErrorCode.SUCCESS.getCode();
        resp.message = ErrorCode.SUCCESS.getDefaultMessage();
        resp.data = data;
        return resp;
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.code = errorCode.getCode();
        resp.message = message;
        return resp;
    }
}
