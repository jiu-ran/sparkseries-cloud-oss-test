package com.sparkseries.module.oss.provider.tencent.factory;

import com.sparkeries.enums.StorageTypeEnum;

import com.sparkseries.module.oss.common.api.provider.service.ValidConnectService;
import com.sparkseries.module.oss.common.api.provider.factory.ConnectValidFactory;
import com.sparkseries.module.oss.provider.tencent.connection.CosValidConnectServiceImpl;
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