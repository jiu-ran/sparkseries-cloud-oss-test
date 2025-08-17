package com.sparkseries.module.oss.provider.aliyun.factory;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.common.api.provider.service.ValidConnectService;
import com.sparkseries.module.oss.common.api.provider.factory.ConnectValidFactory;
import com.sparkseries.module.oss.provider.aliyun.connection.OssValidConnectServiceImpl;
import org.springframework.stereotype.Component;

/**
 * OSS连接验证服务工厂
 */
@Component
public class OssConnectValidFactory implements ConnectValidFactory {

    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.OSS;
    }

    @Override
    public ValidConnectService createValidConnectService() {
        return new OssValidConnectServiceImpl();
    }
}