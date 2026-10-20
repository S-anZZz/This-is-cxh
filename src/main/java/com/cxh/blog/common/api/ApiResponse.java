package com.cxh.blog.common.api;

/**
 * 统一 API 响应包装。通用能力，不含任何业务语义。
 *
 * @param success 是否成功
 * @param message 提示信息
 * @param data    业务数据
 */
public record ApiResponse<T>(boolean success, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "ok", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
