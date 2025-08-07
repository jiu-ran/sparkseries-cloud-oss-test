package com.sparkseries.common.util.tool;

import cn.hutool.core.util.ObjectUtil;
import com.sparkseries.common.util.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 类型转换工具
 */
@Slf4j
public class ObjectConvertTool {
    /**
     * 单个对象转换
     *
     * @param origin      原对象
     * @param targetClass 目标对象类型
     * @return 目标对象
     */
    public static <T, S> S originConvert(T origin, Class<S> targetClass) {
        if (ObjectUtil.isNull(origin)) {
            return null;
        }

        return convert(targetClass, origin);
    }

    /**
     * 集合对象转换
     *
     * @param origins     原对象集合
     * @param targetClass 目标对象类型
     * @return 目标对象集合
     */
    public static <T, S> List<S> originsConvert(List<T> origins, Class<S> targetClass) {
        if (ObjectUtil.isNull(origins)) {
            return null;
        }

        List<S> targetList = new ArrayList<>();
        for (T origin : origins) {
            S target = convert(targetClass, origin);
            targetList.add(target);
        }
        return targetList;
    }

    private static <T, S> S convert(Class<S> targetClass, T origin) {
        S target;
        try {
            target = targetClass.getConstructor().newInstance();
        } catch (Exception e) {
            log.error("实例化目标类失败");
            throw new BusinessException("实例化目标类失败");
        }

        try {
            BeanUtils.copyProperties(origin, target);
        } catch (Exception e) {
            log.error("转换失败");
            throw new BusinessException("转换失败");
        }
        return target;
    }
}
