package com.sparkseries.module.oss.provider.service.impl.aliyun.factory;

import com.sparkseries.module.oss.common.config.PoolConfig;
import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.module.oss.cloud.dao.CloudConfigMapper;
import com.sparkseries.module.oss.cloud.entity.OssConfigEntity;
import com.sparkseries.module.oss.provider.service.impl.aliyun.connection.OssValidConnectServiceImpl;
import com.sparkseries.module.oss.file.dao.FileMetadataMapper;
import com.sparkseries.module.oss.provider.service.base.factory.OssServiceFactory;
import com.sparkseries.module.oss.provider.service.impl.aliyun.pool.OssClientPool;
import com.sparkseries.module.oss.provider.service.base.oss.OssService;
import com.sparkseries.module.oss.provider.service.impl.aliyun.oss.OssOssServiceImpl;
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
            throw new BusinessException("OSS 该配置文件不存在 请先保存再进行切换");
        }

        if (!new OssValidConnectServiceImpl().connectTest(oss.getEndpoint(), oss.getAccessKeyId(), oss.getAccessKeySecret(), oss.getBucketName(), oss.getRegion())) {
            throw new BusinessException("保存的OSS存储配置失效了请重新保存");
        }
        OssClientPool ossClientPool = new OssClientPool(oss.getEndpoint(), oss.getAccessKeyId(), oss.getAccessKeySecret(), oss.getRegion(), poolConfig);
        return new OssOssServiceImpl(ossClientPool, oss.getBucketName(), fileMetadataMapper);
    }
}