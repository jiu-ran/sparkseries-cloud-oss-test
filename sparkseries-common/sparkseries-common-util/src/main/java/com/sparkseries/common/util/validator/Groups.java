package com.sparkseries.common.util.validator;

import jakarta.validation.groups.Default;

/**
 * 校验分组
 */
public interface Groups {
    /**
     * 创建
     */
    interface Create extends Default {}

    /**
     * 删除
     */
    interface Delete extends Default {}

    /**
     * 修改
     */
    interface Update extends Default {}

    /**
     * 查询
     */
    interface Select extends Default {}
}
