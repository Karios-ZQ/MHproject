package com.example.mhproject.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理器：统一异常返回格式，避免堆栈直接暴露给前端。
 * 分布式多实例部署时，前端只看到 {code, message}，日志由后端统一记录。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException e) {
        log.warn("业务异常: {}", e.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 400);
        body.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 静态资源（如 favicon.ico）不存在时按 404 返回，不再当作 500 系统异常污染日志
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException e) {
        log.debug("静态资源不存在: {}", e.getResourcePath());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 404);
        body.put("message", "资源不存在");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception e) {
        log.error("系统异常", e);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 500);
        body.put("message", "服务器内部错误，请稍后重试");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
