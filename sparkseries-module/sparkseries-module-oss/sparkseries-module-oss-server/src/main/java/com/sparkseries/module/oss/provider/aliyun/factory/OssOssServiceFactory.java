package com.sparkseries.module.oss.provider.aliyun.factory;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.cloud.dao.CloudConfigMapper;
import com.sparkseries.module.oss.cloud.entity.OssConfigEntity;
import com.sparkseries.module.oss.common.api.provider.factory.OssServiceFactory;
import com.sparkseries.module.oss.common.api.provider.service.OssService;
import com.sparkseries.module.oss.common.config.PoolConfig;
import com.sparkseries.module.oss.common.exception.OssException;
import com.sparkseries.module.oss.file.dao.FileMetadataMapper;
import com.sparkseries.module.oss.provider.aliyun.connection.OssValidConnectServiceImpl;
import com.sparkseries.module.oss.provider.aliyun.oss.OssOssServiceImpl;
import com.sparkseries.module.oss.provider.aliyun.pool.OssClientPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class OssOssServiceFactory implements OssServiceFactory {

    private final CloudConfigMapper cloudConfigMapper;
    private final PoolConfig poolConfig;
    private final FileMetadataMapper fileMetadataMapper;

    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.OSS;
    }

    @Override
    public OssService createService(Long id) {
        OssConfigEntity oss = cloudConfigMapper.getOssConfigById(id);
        if (ObjectUtils.isEmpty(oss)) {
            throw new OssException("OSS 该配置文件不存在 请先保存再进行切换");
        }

        if (!new OssValidConnectServiceImpl().connectTest(oss.getEndpoint(), oss.getAccessKeyId(), oss.getAccessKeySecret(), oss.getBucketName(), oss.getRegion())) {
            throw new OssException("保存的OSS存储配置失效了请重新保存");
        }
        OssClientPool ossClientPool = new OssClientPool(oss.getEndpoint(), oss.getAccessKeyId(), oss.getAccessKeySecret(), oss.getRegion(), poolConfig);
        return new OssOssServiceImpl(ossClientPool, oss.getBucketName(), fileMetadataMapper);
    }
}