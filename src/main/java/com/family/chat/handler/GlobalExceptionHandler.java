package com.family.chat.handler;

import com.family.chat.common.result.ApiResponse;
import com.family.chat.common.result.ResultCode;
import com.family.chat.exception.ServiceException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常: 可预期的业务规则拦截, 记录 warn 级别即可, 避免污染 error 报警
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiResponse<?>> handleServiceException(ServiceException e) {
        log.warn("Business exception caught. [code={}, message={}]", e.getResultCode().getCode(), e.getResultCode().getMessage());
        return ResponseEntity.ok(ApiResponse.fail(e.getResultCode()));
    }

    /**
     * 参数校验异常(@Valid + @RequestBody): 属于客户端错误, 不应以 500 系统异常对外暴露
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String detail = fieldError != null ? fieldError.getDefaultMessage() : ResultCode.PARAM_ERROR.getMessage();
        log.warn("Request body validation failed. [detail={}]", detail);
        return ResponseEntity.ok(ApiResponse.fail(ResultCode.PARAM_ERROR).message(detail));
    }

    /**
     * 参数校验异常(@Validated + 方法参数约束)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("Method parameter constraint violated. [detail={}]", e.getMessage());
        return ResponseEntity.ok(ApiResponse.fail(ResultCode.PARAM_ERROR));
    }

    /**
     * 请求体解析失败(非法 JSON 等)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("Failed to read request body. [detail={}]", e.getMessage());
        return ResponseEntity.ok(ApiResponse.fail(ResultCode.PARAM_ERROR));
    }

    /**
     * 缺少必传请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("Required request parameter missing. [parameter={}]", e.getParameterName());
        return ResponseEntity.ok(ApiResponse.fail(ResultCode.PARAM_ERROR));
    }

    /**
     * 请求参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Request parameter type mismatch. [name={}, value={}]", e.getName(), e.getValue());
        return ResponseEntity.ok(ApiResponse.fail(ResultCode.PARAM_ERROR));
    }

    /**
     * 兜底未知异常: 必须记录完整堆栈用于排查, 对外屏蔽细节
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Unexpected system exception.", e);
        return ResponseEntity
                .status(500)
                .body(ApiResponse.fail(ResultCode.SYSTEM_ERROR));
    }
}
