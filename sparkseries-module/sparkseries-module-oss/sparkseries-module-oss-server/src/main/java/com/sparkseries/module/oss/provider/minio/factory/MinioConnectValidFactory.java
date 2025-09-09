package com.sparkseries.module.oss.provider.minio.factory;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.common.api.provider.factory.ConnectValidFactory;
import com.sparkseries.module.oss.common.api.provider.service.ValidConnectService;
import com.sparkseries.module.oss.provider.minio.connection.MinioValidConnectServiceImpl;
import org.springframework.stereotype.Component;

/**
 * Minio 连接验证服务工厂
 */
@Component
public class MinioConnectValidFactory implements ConnectValidFactory {
    /**
     * 获取存储类型
     *
     * @return 存储类型枚举
     */
    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.MINIO;
    }

    /**
     * 创建连接验证服务
     *
     * @return 连接验证服务
     */
    @Override
    public ValidConnectService createValidConnectService() {
        return new MinioValidConnectServiceImpl();
    }
}