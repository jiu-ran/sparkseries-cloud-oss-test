package com.sparkseries.module.oss.provider.aliyun.factory;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.common.api.provider.factory.ConnectValidFactory;
import com.sparkseries.module.oss.common.api.provider.service.ValidConnectService;
import com.sparkseries.module.oss.provider.aliyun.connection.OssValidConnectServiceImpl;
import org.springframework.stereotype.Component;

/**
 * OSS 连接校验工厂
 */
@Component
public class OssConnectValidFactory implements ConnectValidFactory {
    /**
     * 获取存储类型
     *
     * @return 存储类型枚举
     */
    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.OSS;
    }

    /**
     * 创建连接校验服务
     *
     * @return 连接校验服务
     */
    @Override
    public ValidConnectService createValidConnectService() {
        return new OssValidConnectServiceImpl();
    }
}