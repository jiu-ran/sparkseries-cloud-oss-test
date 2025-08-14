package com.sparkseries.module.oss.provider.service.impl.minio.factory;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.provider.service.base.connection.ValidConnectService;
import com.sparkseries.module.oss.provider.service.base.factory.ConnectValidFactory;
import com.sparkseries.module.oss.provider.service.impl.minio.connection.MinioValidConnectServiceImpl;
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