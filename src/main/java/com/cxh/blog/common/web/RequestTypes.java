package com.cxh.blog.common.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 判断当前请求期望 JSON 还是 HTML，用于让同一个异常处理器同时服务页面与接口。
 * 放在 common 中，避免各业务域重复实现。
 */
public final class RequestTypes {

    private RequestTypes() {
    }

    public static boolean isApiRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String uri = request.getRequestURI();
        if (uri != null && (uri.startsWith("/api/") || uri.contains("/api/"))) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json") && !accept.contains("text/html");
    }
}
