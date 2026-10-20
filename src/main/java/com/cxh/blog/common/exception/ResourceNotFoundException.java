package com.cxh.blog.common.exception;

/**
 * 目标资源不存在。映射为 HTTP 404。
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
