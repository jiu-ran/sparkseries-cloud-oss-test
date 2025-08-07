package com.sparkseries.common.util.entity;

import com.sparkseries.common.util.tool.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用响应类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "通用响应类")
public class Result<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "响应编码")
    private Integer code;

    @Schema(description = "响应信息")
    private String msg;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "失败常量")
    public final static Integer FAIL = Constants.DEFAULT_ERROR_CODE;

    @Schema(description = "成功常量")
    public final static Integer SUCCESS = Constants.DEFAULT_SUCCESS_CODE;

    public static <T> Result<T> ok() {
        return new Result<>(SUCCESS, null, null);
    }

    public static <T> Result<T> ok(String msg) {
        return new Result<>(SUCCESS, msg, null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(SUCCESS, null, data);
    }

    public static <T> Result<T> ok(String msg, T data) {
        return new Result<>(SUCCESS, msg, data);
    }

    public static <T> Result<T> ok(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> ok(Integer code, String msg, T data) {
        return new Result<>(code, msg, data);
    }

    public static <T> Result<T> error() {
        return new Result<>(FAIL, null, null);
    }

    public static <T> Result<T> error(String msg) {
        return new Result<>(FAIL, msg, null);
    }

    public static <T> Result<T> error(T data) {
        return new Result<>(FAIL, null, data);
    }

    public static <T> Result<T> error(String msg, T data) {
        return new Result<>(FAIL, msg, data);
    }

    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> error(Integer code, String msg, T data) {
        return new Result<>(code, msg, data);
    }

    public static <T> Result<T> error(Integer code, T data) {
        return new Result<>(code, null, data);
    }
}
