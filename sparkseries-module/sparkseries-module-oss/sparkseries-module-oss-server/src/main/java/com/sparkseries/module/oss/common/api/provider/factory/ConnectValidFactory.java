package com.sparkseries.module.oss.common.api.provider.factory;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.common.api.provider.service.ValidConnectService;

/**
 * 连接验证服务工厂接口
 */
public interface ConnectValidFactory {

    /**
     * 获取支持的存储类型
     *
     * @return 存储类型枚举
     */
    StorageTypeEnum getStorageType();

    /**
     * 创建连接验证服务实例
     *
     * @return 连接验证服务
     */
    ValidConnectService createValidConnectService();
}