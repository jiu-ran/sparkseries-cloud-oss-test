package com.sparkseries.module.cloudconfig.factory.valid.impl;

import com.sparkseries.common.enums.StorageTypeEnum;
import com.sparkseries.module.cloudconfig.factory.valid.ValidConnectServiceFactory;
import com.sparkseries.module.cloudconfig.service.connect.ValidConnectService;
import com.sparkseries.module.cloudconfig.service.connect.impl.OssValidConnectServiceImpl;
import org.springframework.stereotype.Component;

/**
 * OSS连接验证服务工厂
 */
@Component
public class OssValidConnectServiceFactory implements ValidConnectServiceFactory {

    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.OSS;
    }

    @Override
    public ValidConnectService createValidConnectService() {
        return new OssValidConnectServiceImpl();
    }
}