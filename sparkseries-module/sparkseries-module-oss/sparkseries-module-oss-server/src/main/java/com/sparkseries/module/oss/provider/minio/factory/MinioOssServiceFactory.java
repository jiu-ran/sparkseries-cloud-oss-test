package com.sparkseries.module.oss.provider.minio.factory;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkeries.enums.VisibilityEnum;
import com.sparkseries.module.oss.cloud.dao.CloudConfigMapper;
import com.sparkseries.module.oss.cloud.entity.MinioConfigEntity;
import com.sparkseries.module.oss.common.api.provider.factory.OssServiceFactory;
import com.sparkseries.module.oss.common.api.provider.service.OssService;
import com.sparkseries.module.oss.common.config.PoolConfig;
import com.sparkseries.module.oss.common.exception.OssException;
import com.sparkseries.module.oss.file.dao.MetadataMapper;
import com.sparkseries.module.oss.provider.minio.connection.MinioValidConnectServiceImpl;
import com.sparkseries.module.oss.provider.minio.oss.MinioOssServiceImpl;
import com.sparkseries.module.oss.provider.minio.pool.MinioClientPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.sparkeries.enums.VisibilityEnum.*;

/**
 * MINIO 存储服务工厂
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioOssServiceFactory implements OssServiceFactory {

    private final CloudConfigMapper cloudConfigMapper;
    private final PoolConfig poolConfig;
    private final MetadataMapper metadataMapper;

    /**
     * 获取存储类型
     *
     * @return 存储类型枚举
     */
    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.MINIO;
    }

    /**
     * 创建存储服务
     *
     * @param id id
     * @return 存储服务
     */
    @Override
    public OssService createService(Long id) {
        MinioConfigEntity minio = cloudConfigMapper.getMinioConfigById(id);
        if (ObjectUtils.isEmpty(minio)) {
            throw new OssException("Minio 该配置文件不存在 请先保存再进行切换");
        }
        Map<VisibilityEnum, String> map = new HashMap<>(3);
        map.put(PUBLIC, minio.getPublicBucketName());
        map.put(PRIVATE, minio.getPrivateBucketName());
        map.put(USER_INFO, minio.getUserInfoBucketName());
        if (!new MinioValidConnectServiceImpl().connectTest(minio.getEndpoint(), minio.getAccessKey(), minio.getSecretKey(), map.get(PUBLIC))) {
            log.warn("MINIO的桶 {} 测试失败", PUBLIC);
            throw new OssException("保存的MINIO存储配置失效了请重新保存");
        }
        if (!new MinioValidConnectServiceImpl().connectTest(minio.getEndpoint(), minio.getAccessKey(), minio.getSecretKey(), map.get(PRIVATE))) {
            log.warn("MINIO的桶 {} 测试失败", PRIVATE);
            throw new OssException("保存的MINIO存储配置失效了请重新保存");
        }
        if (!new MinioValidConnectServiceImpl().connectTest(minio.getEndpoint(), minio.getAccessKey(), minio.getSecretKey(), map.get(USER_INFO))) {
            log.warn("MINIO的桶 {} 测试失败", USER_INFO);
            throw new OssException("保存的MINIO存储配置失效了请重新保存");
        }

        MinioClientPool minioClientPool = new MinioClientPool(minio.getEndpoint(), minio.getAccessKey(), minio.getSecretKey(), poolConfig);
        return new MinioOssServiceImpl(minioClientPool, map, metadataMapper);
    }
}
