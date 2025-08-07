package com.sparkseries.common.util.tool;

/**
 * 通用常量名称
 */
public class Constants {
    /**
     * 默认错误码
     */
    public final static Integer DEFAULT_ERROR_CODE = 500;
    /**
     * 默认成功码
     */
    public final static Integer DEFAULT_SUCCESS_CODE = 200;

    /**
     * 请求令牌过期时间
     */
    public final static Integer ACCESS_TOKEN_TIMEOUT = 2;

    /**
     * 刷新令牌过期时间
     */
    public final static Integer REFRESH_TOKEN_TIMEOUT = 14;

    /**
     * 注册/登录 验证码过期时间
     */
    public final static Integer CODE_TIMEOUT = 50 * 60;

    /**
     * 未删除
     */
    public final static Integer NOT_DELETED = 0;

    /**
     * 是否是管理员
     */
    public final static Integer IS_ADMIN = 1;

    /**
     * 数据权限范围
     */
    public final static String DATA_SCOPE = "dataScope";

    /**
     * 当前页面
     */
    public final static String CURRENT = "current";

    /**
     * 成功响应
     */
    public final static String OK = "OK";

    /**
     * AI 会话标题
     */
    public final static String TITLE = "新会话";

    /**
     * BPMN 文件后缀
     */
    public final static String BPMN = ".bpmn";

    /**
     * BPMN 文件后缀
     */
    public final static String BPMN20 = ".bpmn20.xml";

    /**
     * 存储类型枚举
     */
    public final static String STORAGE_TYPE_ENUM = "StorageTypeEnum";
}
