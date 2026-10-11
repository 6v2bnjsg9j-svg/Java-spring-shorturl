package com.wcy.shorturl.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：异常分类 + 统一响应格式，避免堆栈直接暴露给客户端。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：按错误码映射 HTTP 状态 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException e) {
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.fail(e.getErrorCode(), e.getMessage()));
    }

    /** 参数校验失败：取第一条校验信息返回 400 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().isEmpty()
                ? ErrorCode.PARAM_INVALID.getDefaultMessage()
                : e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity
                .status(400)
                .body(ApiResponse.fail(ErrorCode.PARAM_INVALID, msg));
    }

    /** 兜底：未知异常记录日志，对外只暴露通用信息（不泄露内部细节） */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e) {
        log.error("系统内部异常: ", e);
        return ResponseEntity
                .status(500)
                .body(ApiResponse.fail(ErrorCode.SYSTEM_ERROR, ErrorCode.SYSTEM_ERROR.getDefaultMessage()));
    }
}
