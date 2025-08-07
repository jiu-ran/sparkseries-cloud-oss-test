package com.sparkseries.common.util.exception;

import com.sparkseries.common.util.tool.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 自定义校验异常处理类
 */
public class ValidationException extends RuntimeException {

    // 默认错误码

    private static final Integer DEFAULT_ERROR_CODE = Constants.DEFAULT_ERROR_CODE;
    @Getter
    @Schema(description = "错误码")
    private final Integer code;
    @Schema(description = "错误信息")
    private final String message;

    /**
     * 仅包含错误信息
     */
    public ValidationException(String message) {
        super(message);
        this.code = DEFAULT_ERROR_CODE;
        this.message = message;
    }

    /**
     * 包含错误码和错误信息
     */
    public ValidationException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 包含错误信息和原始异常 (用于包装底层异常)
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.code = DEFAULT_ERROR_CODE;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
