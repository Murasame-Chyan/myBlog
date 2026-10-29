package com.murasame.handler;

import com.murasame.security.PermissionDeniedException;
import com.murasame.util.ReturnUtil;
import jakarta.validation.ConstraintViolationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ResponseBody
    @ExceptionHandler(ConstraintViolationException.class)
    public Map<String, Object> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse("参数校验失败");
        return ReturnUtil.badRequest(message);
    }

    @ResponseBody
    @ExceptionHandler(PermissionDeniedException.class)
    public Map<String, Object> handlePermissionDenied(PermissionDeniedException e) {
        return ReturnUtil.forbidden(e.getMessage());
    }

    // @RequestBody + @Valid 校验失败：默认会走 Spring 的 400 响应，这里统一成 ReturnUtil 结构
    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        return ReturnUtil.badRequest(firstFieldMessage(ex.getBindingResult().getFieldErrors()));
    }

    // 表单/查询参数绑定失败（@ModelAttribute、@RequestParam 对象）
    @ResponseBody
    @ExceptionHandler(BindException.class)
    public Map<String, Object> handleBindException(BindException ex) {
        return ReturnUtil.badRequest(firstFieldMessage(ex.getBindingResult().getFieldErrors()));
    }

    private static String firstFieldMessage(java.util.List<org.springframework.validation.FieldError> errors) {
        return errors.stream()
                .findFirst()
                .map(org.springframework.validation.FieldError::getDefaultMessage)
                .orElse("参数校验失败");
    }
}
