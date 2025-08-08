package com.sparkseries.module.storage.factory.storage.impl;

import com.sparkseries.common.config.PoolConfig;
import com.sparkseries.common.enums.StorageTypeEnum;
import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.module.cloudconfig.dao.CloudConfigMapper;
import com.sparkseries.module.cloudconfig.entity.KodoConfigEntity;
import com.sparkseries.module.cloudconfig.service.connect.impl.KodoValidConnectServiceImpl;
import com.sparkseries.module.file.dao.FileMetadataMapper;
import com.sparkseries.module.storage.factory.storage.OssServiceFactory;
import com.sparkseries.module.storage.pool.KodoClientPool;
import com.sparkseries.module.storage.service.oss.OssService;
import com.sparkseries.module.storage.service.oss.impl.KodoOssServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class KodoOssServiceFactory implements OssServiceFactory {

    private final CloudConfigMapper cloudConfigMapper;
    private final PoolConfig poolConfig;
    private final FileMetadataMapper fileMetadataMapper;

    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.KODO;
    }

    @Override
    public OssService createService(Long id) {
        KodoConfigEntity kodo = cloudConfigMapper.getKodoConfigById(id);
        if (ObjectUtils.isEmpty(kodo)) {
            throw new BusinessException("KODO 该配置文件不存在 请先保存再进行切换");
        }
        if (!new KodoValidConnectServiceImpl().connectTest(kodo.getAccessKey(), kodo.getSecretKey(), kodo.getBucketName())) {
            throw new BusinessException("保存的KODO存储配置失效了请重新保存");
        }
        KodoClientPool kodoClientPool = new KodoClientPool(kodo.getAccessKey(), kodo.getSecretKey(), poolConfig);

        return new KodoOssServiceImpl(kodoClientPool, kodo.getBucketName(),fileMetadataMapper);
    }
}
