package com.sparkseries.module.storage.factory.storage.impl;

import com.sparkseries.common.config.PoolConfig;
import com.sparkseries.common.enums.StorageTypeEnum;
import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.module.cloudconfig.dao.CloudConfigMapper;
import com.sparkseries.module.cloudconfig.entity.CosConfigEntity;
import com.sparkseries.module.cloudconfig.service.connect.impl.CosValidConnectServiceImpl;
import com.sparkseries.module.file.dao.FileMetadataMapper;
import com.sparkseries.module.storage.factory.storage.OssServiceFactory;
import com.sparkseries.module.storage.pool.CosClientPool;
import com.sparkseries.module.storage.service.oss.OssService;
import com.sparkseries.module.storage.service.oss.impl.CosOssServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class CosOssServiceFactory implements OssServiceFactory {

    private final CloudConfigMapper cloudConfigMapper;
    private final PoolConfig poolConfig;
    private final FileMetadataMapper fileMetadataMapper;

    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.COS;
    }

    @Override
    public OssService createService(Long id) {
        CosConfigEntity cos = cloudConfigMapper.getCosConfigById(id);
        if (ObjectUtils.isEmpty(cos)) {
            throw new BusinessException("COS 该配置文件不存在 请先保存再进行切换");
        }
        if (!new CosValidConnectServiceImpl().connectTest(cos.getSecretId(), cos.getSecretKey(), cos.getBucketName(),
                cos.getRegion())) {
            throw new BusinessException("保存的COS存储配置失效了请重新保存");
        }
        CosClientPool cosClientPool = new CosClientPool(cos.getSecretId(), cos.getSecretKey(), cos.getRegion(),
                cos.getBucketName(), poolConfig);
        return new CosOssServiceImpl(cosClientPool, cos.getBucketName(), fileMetadataMapper);
    }
}