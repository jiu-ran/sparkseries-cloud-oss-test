package com.sparkseries.module.oss.provider.qiniu.factory;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.common.api.provider.factory.ConnectValidFactory;
import com.sparkseries.module.oss.common.api.provider.service.ValidConnectService;
import com.sparkseries.module.oss.provider.qiniu.connection.KodoValidConnectServiceImpl;
import org.springframework.stereotype.Component;

/**
 * Kodo 连接校验工厂
 */
@Component
public class KodoConnectValidFactory implements ConnectValidFactory {
    /**
     * 获取存储类型
     *
     * @return 存储类型枚举
     */
    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.KODO;
    }

    /**
     * 创建连接校验服务
     *
     * @return 连接校验服务
     */
    @Override
    public ValidConnectService createValidConnectService() {
        return new KodoValidConnectServiceImpl();
    }
}