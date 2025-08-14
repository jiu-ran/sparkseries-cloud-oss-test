package com.sparkseries.module.oss.provider.service.impl.qiniu.factory;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.provider.service.base.connection.ValidConnectService;
import com.sparkseries.module.oss.provider.service.base.factory.ConnectValidFactory;
import com.sparkseries.module.oss.provider.service.impl.qiniu.connection.KodoValidConnectServiceImpl;
import org.springframework.stereotype.Component;

/**
 * Kodo连接验证服务工厂
 */
@Component
public class KodoConnectValidFactory implements ConnectValidFactory {

    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.KODO;
    }

    @Override
    public ValidConnectService createValidConnectService() {
        return new KodoValidConnectServiceImpl();
    }
}