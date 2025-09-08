package com.sparkeries.enums;

import lombok.Getter;

/**
 * 定义文件的可见性
 */
@Getter
public enum VisibilityEnum {
    /**
     * 私有
     */
    PRIVATE("private"),

    /**
     * 公开
     */
    PUBLIC("public"),

    /**
     * 用户信息
     */

    USER_AVATAR("userAvatar");

    private final String key;

    VisibilityEnum(String key) {
        this.key = key;
    }
}
