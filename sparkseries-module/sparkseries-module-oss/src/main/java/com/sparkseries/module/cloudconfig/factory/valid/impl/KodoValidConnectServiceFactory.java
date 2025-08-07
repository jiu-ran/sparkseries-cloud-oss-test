package com.sparkseries.module.cloudconfig.factory.valid.impl;

import com.sparkseries.common.enums.StorageTypeEnum;
import com.sparkseries.module.cloudconfig.factory.valid.ValidConnectServiceFactory;
import com.sparkseries.module.cloudconfig.service.connect.ValidConnectService;
import com.sparkseries.module.cloudconfig.service.connect.impl.KodoValidConnectServiceImpl;
import org.springframework.stereotype.Component;

/**
 * Kodo连接验证服务工厂
 */
@Component
public class KodoValidConnectServiceFactory implements ValidConnectServiceFactory {

    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.KODO;
    }

    @Override
    public ValidConnectService createValidConnectService() {
        return new KodoValidConnectServiceImpl();
    }
}