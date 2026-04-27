package com.lab.file.common.handler;

import com.lab.file.common.exception.BusinessException;
import com.lab.file.common.response.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        logger.error("业务异常: {}", e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(javax.validation.ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(javax.validation.ConstraintViolationException e) {
        logger.error("参数验证异常: {}", e.getMessage(), e);
        StringBuilder errorMsg = new StringBuilder();
        e.getConstraintViolations().forEach(violation -> {
            errorMsg.append(violation.getMessage()).append("; ");
        });
        return Result.error(400, "参数验证失败: " + errorMsg.toString());
    }

    /**
     * 处理方法参数异常
     */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e) {
        logger.error("方法参数类型不匹配异常: {}", e.getMessage(), e);
        return Result.error(400, "参数类型错误: " + e.getName() + " 参数类型不正确");
    }

    /**
     * 处理请求参数绑定异常
     */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(org.springframework.web.bind.MissingServletRequestParameterException e) {
        logger.error("缺少请求参数异常: {}", e.getMessage(), e);
        return Result.error(400, "缺少必要参数: " + e.getParameterName());
    }

    /**
     * 处理请求映射异常
     */
    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    public Result<Void> handleNoHandlerFoundException(org.springframework.web.servlet.NoHandlerFoundException e) {
        logger.error("请求路径不存在: {}", e.getRequestURL(), e);
        return Result.error(404, "请求路径不存在");
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        logger.error("系统异常: {}", e.getMessage(), e);
        return Result.error(500, "系统内部错误，请联系管理员");
    }
}