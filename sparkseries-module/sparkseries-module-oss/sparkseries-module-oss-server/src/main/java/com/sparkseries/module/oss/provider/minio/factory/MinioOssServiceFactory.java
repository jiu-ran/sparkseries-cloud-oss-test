package com.sparkseries.module.oss.provider.minio.factory;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.cloud.dao.CloudConfigMapper;
import com.sparkseries.module.oss.cloud.entity.MinioConfigEntity;
import com.sparkseries.module.oss.common.api.provider.factory.OssServiceFactory;
import com.sparkseries.module.oss.common.api.provider.service.OssService;
import com.sparkseries.module.oss.common.config.PoolConfig;
import com.sparkseries.module.oss.common.exception.OssException;
import com.sparkseries.module.oss.file.dao.FileMetadataMapper;
import com.sparkseries.module.oss.provider.minio.connection.MinioValidConnectServiceImpl;
import com.sparkseries.module.oss.provider.minio.oss.MinioOssServiceImpl;
import com.sparkseries.module.oss.provider.minio.pool.MinioClientPool;
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
            throw new OssException("Minio 该配置文件不存在 请先保存再进行切换");
        }
        if (!new MinioValidConnectServiceImpl().connectTest(minio.getEndpoint(), minio.getAccessKey(), minio.getSecretKey(), minio.getBucketName())) {
            throw new OssException("保存的MINIO存储配置失效了请重新保存");
        }
        MinioClientPool minioClientPool = new MinioClientPool(minio.getEndpoint(), minio.getAccessKey(), minio.getSecretKey(), poolConfig);
        return new MinioOssServiceImpl(minioClientPool, minio.getBucketName(), fileMetadataMapper);
    }
}
