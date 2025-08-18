package com.sparkseries.common.util.handler;

import static com.sparkseries.common.util.tool.Constants.STORAGE_TYPE_ENUM;

import com.sparkseries.common.util.entity.Result;
import com.sparkseries.common.util.exception.BaseException;
import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.common.util.exception.ValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理枚举类型转换异常 当输入的枚举值不存在时，Spring会抛出MethodArgumentTypeMismatchException
     */

    private Result<?> buildErrorResponse(Exception e, HttpStatus status, String logMessage) {
        log.error(logMessage, e);
        return Result.error(status.value(), e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleJsonParseError(HttpMessageNotReadableException ex) {
        if (ex.getMessage().contains(STORAGE_TYPE_ENUM)) {
            return Result.error("请选择正确的服务类型，支持的类型：OSS、COS、KODO、MINIO、LOCAL");
        }
        return Result.error("请求数据格式错误");
    }

    /**
     * 处理 BaseException 及其子类异常
     *
     * @param e BaseException 异常
     * @return 默认响应类
     */
    @ExceptionHandler(BaseException.class)
    public Result<?> handleBaseException(BaseException e) {
        // 根据异常类型提供更具体的日志信息
        String exceptionType = e.getClass().getSimpleName();
        log.warn("捕获到异常: [类型: {}], [错误码: {}], [信息: '{}']", exceptionType, e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleEnumTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("枚举类型转换异常: {}", ex.getMessage());

        // 检查是否是StorageTypeEnum相关的异常
        if (ex.getRequiredType() != null && ex.getRequiredType().getSimpleName()
                .equals(STORAGE_TYPE_ENUM)) {
            return Result.error("请选择正确的服务类型，支持的类型：OSS、COS、KODO、MINIO、LOCAL");
        }

        // 其他枚举类型异常的通用处理
        return Result.error("参数类型错误: " + ex.getName());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> errors = new ArrayList<>();

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            // 只取 message 部分，忽略 propertyPath
            errors.add(violation.getMessage());
        }

        return Result.error(String.join(", ", errors));
    }

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code = {}, message = {}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public Result<?> handleValidationException(ValidationException e) {
        return buildErrorResponse(e, HttpStatus.BAD_REQUEST, "校验失败: message = {}");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();

        // 合并同一字段的多个错误
        Map<String, List<String>> errorMap = fieldErrors.stream().collect(
                Collectors.groupingBy(FieldError::getField, Collectors.mapping(
                        error -> Optional.ofNullable(error.getDefaultMessage()).orElse("参数无效"),
                        Collectors.toList())));

        log.warn("参数校验失败: {}", errorMap);
        return Result.error(HttpStatus.BAD_REQUEST.value(), errorMap);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        return buildErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR, "系统异常: message = {}");
    }
}