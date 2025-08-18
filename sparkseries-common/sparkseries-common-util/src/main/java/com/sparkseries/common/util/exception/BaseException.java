package com.sparkseries.common.util.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 异常抽象基类
 */
@Getter
public abstract class BaseException extends RuntimeException {
    @Schema(description = "错误码")
    private final Integer code;

    /**
     * 核心构造函数
     *
     * @param code    错误码
     * @param message 错误信息
     */
    public BaseException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 包装底层异常的构造函数
     *
     * @param code    错误码
     * @param message 错误信息
     * @param cause   原始异常
     */
    public BaseException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}