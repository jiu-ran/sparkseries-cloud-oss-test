package com.sparkseries.common.util.validator;

import java.util.regex.Pattern;

/**
 * 密码强度校验工具
 */
public class PasswordStrengthValidator {

    // 定义正则表达式
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]+");

    public static String validatePassword(String password) {
        if (password.length() < 8) {
            return "密码长度必须至少 8 个字符";
        }
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            return "密码必须包含至少一个大写字母";
        }
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            return "密码必须包含至少一个小写字母";
        }
        if (!DIGIT_PATTERN.matcher(password).find()) {
            return "密码必须包含至少一个数字";
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            return "密码必须包含至少一个特殊字符";
        }
        return "密码强度符合要求";
    }
}