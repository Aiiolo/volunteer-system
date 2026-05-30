package com.sanjuan.volunteer.exception;

import com.sanjuan.volunteer.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidation(Exception ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail("请求参数不合法"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail("无权访问该资源"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex, HttpServletRequest request) {
        // 记录完整异常，但响应不泄露堆栈和内部实现细节。
        log.error("Unhandled exception, path={}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("服务器内部错误"));
    }
}
