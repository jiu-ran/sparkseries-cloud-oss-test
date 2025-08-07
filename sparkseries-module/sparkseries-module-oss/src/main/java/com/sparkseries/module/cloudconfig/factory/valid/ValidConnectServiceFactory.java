package com.sparkseries.module.cloudconfig.factory.valid;

import com.sparkseries.common.enums.StorageTypeEnum;
import com.sparkseries.module.cloudconfig.service.connect.ValidConnectService;

/**
 * 连接验证服务工厂接口
 */
public interface ValidConnectServiceFactory {

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