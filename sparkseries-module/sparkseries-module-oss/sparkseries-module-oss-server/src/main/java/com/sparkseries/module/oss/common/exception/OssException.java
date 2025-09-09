package com.sparkseries.module.oss.common.exception;

import cn.hutool.http.HttpStatus;
import com.sparkseries.common.util.exception.BaseException;

/**
 * 对象存储异常
 */
public class OssException extends BaseException {
    /**
     * 仅包含错误信息(使用默认错误码)
     *
     * @param message 错误信息
     */
    public OssException(String message) {
        super(HttpStatus.HTTP_INTERNAL_ERROR, message);
    }

    /**
     * 包含错误码和错误信息
     *
     * @param code 错误码
     * @param message 错误信息
     */
    public OssException(Integer code, String message) {
        super(code, message);
    }

    /**
     * 包含错误信息和原始异常(使用默认错误码)
     *
     * @param message 错误信息
     * @param cause 原始异常
     */
    public OssException(String message, Throwable cause) {
        super(HttpStatus.HTTP_INTERNAL_ERROR, message, cause);
    }

    /**
     * 包含错误码、错误信息和原始异常
     *
     * @param code 错误码
     * @param message 错误信息
     * @param cause 原始异常
     */
    public OssException(Integer code, String message, Throwable cause) {
        super(code, message, cause);
    }

}