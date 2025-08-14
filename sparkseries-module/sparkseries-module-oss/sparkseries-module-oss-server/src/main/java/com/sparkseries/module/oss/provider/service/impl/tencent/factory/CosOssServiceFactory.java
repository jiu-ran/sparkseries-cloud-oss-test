package com.sparkseries.module.oss.provider.service.impl.tencent.factory;

import com.sparkseries.module.oss.common.config.PoolConfig;
import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.module.oss.cloud.dao.CloudConfigMapper;
import com.sparkseries.module.oss.cloud.entity.CosConfigEntity;
import com.sparkseries.module.oss.provider.service.impl.tencent.connection.CosValidConnectServiceImpl;
import com.sparkseries.module.oss.file.dao.FileMetadataMapper;
import com.sparkseries.module.oss.provider.service.base.factory.OssServiceFactory;
import com.sparkseries.module.oss.provider.service.impl.tencent.pool.CosClientPool;
import com.sparkseries.module.oss.provider.service.base.oss.OssService;
import com.sparkseries.module.oss.provider.service.impl.tencent.oss.CosOssServiceImpl;
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
        if (!new CosValidConnectServiceImpl().connectTest(cos.getSecretId(), cos.getSecretKey(), cos.getBucketName(), cos.getRegion())) {
            throw new BusinessException("保存的COS存储配置失效了请重新保存");
        }
        CosClientPool cosClientPool = new CosClientPool(cos.getSecretId(), cos.getSecretKey(), cos.getRegion(), cos.getBucketName(), poolConfig);
        return new CosOssServiceImpl(cosClientPool, cos.getBucketName(), fileMetadataMapper);
    }
}