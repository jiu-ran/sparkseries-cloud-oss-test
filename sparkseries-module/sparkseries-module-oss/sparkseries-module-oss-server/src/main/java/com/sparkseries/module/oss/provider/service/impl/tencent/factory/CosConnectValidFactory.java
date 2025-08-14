package com.sparkseries.module.oss.provider.service.impl.tencent.factory;

import com.sparkeries.enums.StorageTypeEnum;

import com.sparkseries.module.oss.provider.service.base.connection.ValidConnectService;
import com.sparkseries.module.oss.provider.service.base.factory.ConnectValidFactory;
import com.sparkseries.module.oss.provider.service.impl.tencent.connection.CosValidConnectServiceImpl;
import org.springframework.stereotype.Component;

/**
 * COS连接验证服务工厂
 */
@Component
public class CosConnectValidFactory implements ConnectValidFactory {
    
    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.COS;
    }
    
    @Override
    public ValidConnectService createValidConnectService() {
        return new CosValidConnectServiceImpl();
    }
}