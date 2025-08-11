package com.sparkseries.module.provider.minio.factory;

import com.sparkseries.common.config.PoolConfig;
import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.module.cloudconfig.dao.CloudConfigMapper;
import com.sparkseries.module.cloudconfig.entity.MinioConfigEntity;
import com.sparkseries.module.provider.minio.connection.MinioValidConnectServiceImpl;
import com.sparkseries.module.file.dao.FileMetadataMapper;
import com.sparkseries.module.storage.factory.storage.OssServiceFactory;
import com.sparkseries.module.provider.minio.pool.MinioClientPool;
import com.sparkseries.module.storage.service.oss.OssService;
import com.sparkseries.module.provider.minio.service.MinioOssServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class MinioOssServiceFactory implements OssServiceFactory {

    private final CloudConfigMapper cloudConfigMapper;
    private final PoolConfig poolConfig;
    private final FileMetadataMapper fileMetadataMapper;

    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.MINIO;
    }

    @Override
    public OssService createService(Long id) {
        MinioConfigEntity minio = cloudConfigMapper.getMinioConfigById(id);
        if (ObjectUtils.isEmpty(minio)) {
            throw new BusinessException("Minio 该配置文件不存在 请先保存再进行切换");
        }
        if (!new MinioValidConnectServiceImpl().connectTest(minio.getEndpoint(), minio.getAccessKey(), minio.getSecretKey(), minio.getBucketName())) {
            throw new BusinessException("保存的MINIO存储配置失效了请重新保存");
        }
        MinioClientPool minioClientPool = new MinioClientPool(minio.getEndpoint(), minio.getAccessKey(), minio.getSecretKey(), poolConfig);
        return new MinioOssServiceImpl(minioClientPool, minio.getBucketName(), fileMetadataMapper);
    }
}
