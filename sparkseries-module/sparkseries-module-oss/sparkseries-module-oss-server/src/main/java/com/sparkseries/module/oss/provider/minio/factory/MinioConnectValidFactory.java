package com.sparkseries.module.oss.provider.minio.factory;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.common.api.provider.service.ValidConnectService;
import com.sparkseries.module.oss.common.api.provider.factory.ConnectValidFactory;
import com.sparkseries.module.oss.provider.minio.connection.MinioValidConnectServiceImpl;
import org.springframework.stereotype.Component;

/**
 * Minio连接验证服务工厂
 */
@Component
public class MinioConnectValidFactory implements ConnectValidFactory {

    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.MINIO;
    }

    @Override
    public ValidConnectService createValidConnectService() {
        return new MinioValidConnectServiceImpl();
    }
}