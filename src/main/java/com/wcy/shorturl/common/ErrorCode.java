package com.wcy.shorturl.common;

/**
 * 统一错误码
 */
public enum ErrorCode {

    SUCCESS(200, 200, "成功"),
    PARAM_INVALID(400, 400, "参数错误"),
    URL_INVALID(400, 400, "长链接格式非法，仅支持 http/https"),
    URL_TOO_LONG(400, 400, "长链接长度超出限制(2048)"),
    URL_NOT_FOUND(404, 404, "短链不存在或已过期"),
    TOO_MANY_REQUESTS(429, 429, "请求过于频繁，请稍后再试"),
    SYSTEM_ERROR(500, 500, "系统内部错误");

    /** 响应体中的业务码 */
    private final int code;
    /** HTTP 状态码 */
    private final int httpStatus;
    private final String defaultMessage;

    ErrorCode(int code, int httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
