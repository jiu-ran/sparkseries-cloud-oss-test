package com.sparkseries.module.storage.factory.storage.impl;

import com.sparkseries.common.config.PoolConfig;
import com.sparkseries.common.enums.StorageTypeEnum;
import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.module.cloudconfig.dao.CloudConfigMapper;
import com.sparkseries.module.cloudconfig.entity.OssConfigEntity;
import com.sparkseries.module.cloudconfig.service.connect.impl.OssValidConnectServiceImpl;
import com.sparkseries.module.file.dao.FileMetadataMapper;
import com.sparkseries.module.storage.factory.storage.OssServiceFactory;
import com.sparkseries.module.storage.pool.OssClientPool;
import com.sparkseries.module.storage.service.oss.OssService;
import com.sparkseries.module.storage.service.oss.impl.OssOssServiceImpl;
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
        // 建议将 connectTest 也作为 Bean 注入，而不是每次都 new
        if (!new OssValidConnectServiceImpl().connectTest(oss.getEndpoint(), oss.getAccessKeyId(),
                oss.getAccessKeySecret(), oss.getBucketName(), oss.getRegion())) {
            throw new BusinessException("保存的OSS存储配置失效了请重新保存");
        }
        OssClientPool ossClientPool = new OssClientPool(oss.getEndpoint(), oss.getAccessKeyId(),
                oss.getAccessKeySecret(), oss.getRegion(), poolConfig);
        return new OssOssServiceImpl(ossClientPool, oss.getBucketName(), fileMetadataMapper);
    }
}