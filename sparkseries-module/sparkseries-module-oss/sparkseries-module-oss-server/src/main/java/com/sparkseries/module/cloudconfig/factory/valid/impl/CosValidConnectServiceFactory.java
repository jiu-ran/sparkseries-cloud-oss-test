package com.sparkseries.module.cloudconfig.factory.valid.impl;

import com.sparkeries.enums.StorageTypeEnum;

import com.sparkseries.module.cloudconfig.factory.valid.ValidConnectServiceFactory;
import com.sparkseries.module.cloudconfig.service.connect.ValidConnectService;
import com.sparkseries.module.provider.tencent.connection.CosValidConnectServiceImpl;
import org.springframework.stereotype.Component;

/**
 * COS连接验证服务工厂
 */
@Component
public class CosValidConnectServiceFactory implements ValidConnectServiceFactory {
    
    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.COS;
    }
    
    @Override
    public ValidConnectService createValidConnectService() {
        return new CosValidConnectServiceImpl();
    }
}