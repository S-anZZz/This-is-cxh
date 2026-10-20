package com.cxh.blog.common.exception;

/**
 * 业务规则被违反时抛出（如非法颜色值、未发布的文章被访问）。
 * 由 {@link GlobalExceptionHandler} 统一翻译为 HTTP 响应。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
