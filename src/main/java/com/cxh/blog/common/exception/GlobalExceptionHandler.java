package com.cxh.blog.common.exception;

import com.cxh.blog.common.api.ApiResponse;
import com.cxh.blog.common.web.RequestTypes;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常翻译：把领域异常统一映射为 HTTP 语义。
 *
 * <p>同一个处理器同时服务两类客户端：
 * 请求 {@code Accept: application/json}（或 {@code /api/**}）返回统一 JSON，
 * 其余返回带站点风格的错误页面。业务代码因此无需关心响应形态。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return respond(ex.getMessage(), HttpStatus.NOT_FOUND, "error", request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleMissingResource(NoResourceFoundException ex, HttpServletRequest request) {
        return respond("页面不存在：" + ex.getResourcePath(), HttpStatus.NOT_FOUND, "error", request, null);
    }

    @ExceptionHandler(BusinessException.class)
    public Object handleBusiness(BusinessException ex, HttpServletRequest request) {
        return respond(ex.getMessage(), HttpStatus.BAD_REQUEST, "error", request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return respond(ex.getMessage(), HttpStatus.BAD_REQUEST, "error", request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return respond("参数校验失败", HttpStatus.BAD_REQUEST, "error", request, errors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return respond("没有访问权限", HttpStatus.FORBIDDEN, "error", request, null);
    }

    /**
     * 路径变量/查询参数类型不匹配（例如 {@code ?status=INVALID}、{@code /admin/posts/abc/edit}、
     * {@code ?page=abc}）。这类输入是用户或爬虫可随意构造的，必须返回 400 而不是 500。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Object handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return respond("参数「" + ex.getName() + "」格式不正确", HttpStatus.BAD_REQUEST, "error", request, null);
    }

    /** 不受支持的 HTTP 方法（例如对只允许 POST 的地址发 GET）。 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                           HttpServletRequest request) {
        return respond("该地址不支持 " + ex.getMethod() + " 方法",
                HttpStatus.METHOD_NOT_ALLOWED, "error", request, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Object handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                              HttpServletRequest request) {
        return respond("不支持的请求内容类型", HttpStatus.UNSUPPORTED_MEDIA_TYPE, "error", request, null);
    }

    /**
     * 数据库约束冲突。最典型的是并发保存同名文章时的 slug 唯一键竞态：
     * 应用层的查重无法覆盖并发窗口，由数据库兜底，这里把它翻译成 409 而不是 500。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("数据完整性冲突：{} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond("保存冲突：链接标识可能已被占用，请返回修改后重试",
                HttpStatus.CONFLICT, "error", request, null);
    }

    @ExceptionHandler(Exception.class)
    public Object handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("未预期的异常：{} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond("服务器内部错误，请稍后再试", HttpStatus.INTERNAL_SERVER_ERROR, "error", request, null);
    }

    private Object respond(String message,
                           HttpStatus status,
                           String viewName,
                           HttpServletRequest request,
                           Map<String, String> details) {
        if (RequestTypes.isApiRequest(request)) {
            ApiResponse<Object> body = details == null || details.isEmpty()
                    ? ApiResponse.fail(message)
                    : new ApiResponse<>(false, message, details);
            return ResponseEntity.status(status).body(body);
        }
        ModelAndView view = new ModelAndView(viewName, status);
        view.addObject("status", status.value());
        view.addObject("message", message);
        if (details != null && !details.isEmpty()) {
            view.addObject("details", details);
        }
        return view;
    }
}
