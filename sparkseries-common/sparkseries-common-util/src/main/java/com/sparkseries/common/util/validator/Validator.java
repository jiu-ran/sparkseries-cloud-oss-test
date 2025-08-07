package com.sparkseries.common.util.validator;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.sparkseries.common.util.exception.ValidationException;

import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 自定义校验器
 */
public class Validator {
    private static final String EMAIL_ADDRESS_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String DEFAULT_DATE_PATTERN = "yyyy:MM:dd HH:ss:mm";

    /**
     * 校验异常
     */
    private static void addError(String errorMsg) {
        throw new ValidationException(errorMsg);
    }

    /**
     * 校验 Object
     */
    public static void validateObject(Object o, String errorMsg) {
        if (ObjectUtil.isEmpty(o)) {
            addError(errorMsg);
        }
    }

    /**
     * 校验字符串
     */
    public static void validateStringValue(String fild, Integer minLen, Integer maxLen, String errorMsg) {
        if (ObjectUtil.isNull(fild) || fild.length() < minLen || fild.length() > maxLen) {
            addError(errorMsg);
        }
    }

    /**
     * 校验 Integer
     */
    public static void validateIntegerValue(Integer fild, Integer min, Integer max, String errorMsg) {
        if (Objects.isNull(fild) || fild < min || fild > max) {
            addError(errorMsg);
        }
    }

    /**
     * 校验 Long
     */
    public static void validateLongValue(Long fild, Long min, Long max, String errorMsg) {
        if (Objects.isNull(fild) || fild < min || fild > max) {
            addError(errorMsg);
        }
    }

    /**
     * 校验 Double
     */
    public static void validateDoubleValue(Double fild, Double min, Double max, String errorMsg) {
        if (Objects.isNull(fild) || fild < min || fild > max) {
            addError(errorMsg);
        }
    }

    /**
     * 校验邮箱
     */
    public static void validateEmail(String email, String errorMsg) {
        if (StrUtil.isBlank(email) || !Pattern.matches(EMAIL_ADDRESS_PATTERN, email)) {
            addError(errorMsg);
        }
    }

    /**
     * 校验日期
     */
    public static void validateDate(String date, String errorMsg) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DEFAULT_DATE_PATTERN);
        try {
            formatter.parse(date);
        } catch (DateTimeException e) {
            addError(errorMsg);
        }
    }

    /**
     * 校验布尔值
     */
    public static void validateBoolean(String filed, String errorMsg) {
        if (StrUtil.isBlank(filed)) {
            addError(errorMsg);
            return;
        }

        filed = filed.trim().toLowerCase();
        if ("0".equals(filed) || "false".equals(filed)) {
            return;
        } else if ("1".equals(filed) || "true".equals(filed)) {
            return;
        }

        addError(errorMsg);
    }
}
