package com.sparkseries.common.util.tool;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 判空工具
 * TODO 已被替换 后期可以修改
 */
public class NullChecker {
    /**
     * 判断对象是否为空 (支持 String/Collection/Map/Array/Optional)
     *
     * @param value 对象
     * @return 为空返回 true, 否则返回 false
     */
    public static <T> boolean isEmpty(T value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).trim().isEmpty();
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).isEmpty();
        }
        if (value instanceof Map) {
            return ((Map<?, ?>) value).isEmpty();
        }
        if (value instanceof Optional) {
            return ((Optional<?>) value).isEmpty();
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value) == 0;
        }
        return false;
    }
}
