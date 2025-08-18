package com.sparkseries.common.security.util;

import java.util.Random;

/**
 * 当前操作用户
 */
public class CurrentUser {
    /**
     * 匿名用户
     */
    private static final String ANONYMOUS_USER = "anonymousUser";


    /**
     * 获取当前用户 ID
     *
     * @return 当前用户的 ID
     */
    public static Long getId() {
        Random random = new Random();

        return random.nextLong();
    }
}