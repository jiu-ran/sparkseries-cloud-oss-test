package com.sparkseries.module.oss.provider.minio.factory;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.cloud.dao.CloudConfigMapper;
import com.sparkseries.module.oss.cloud.dto.CloudConfigDTO;
import com.sparkseries.module.oss.cloud.entity.MinioConfigEntity;
import com.sparkseries.module.oss.common.api.provider.factory.OssConfigFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Minio 配置保存策略
 */
@Component
@RequiredArgsConstructor
public class MinioOssConfigFactory implements OssConfigFactory {

    private final CloudConfigMapper cloudMapper;

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
     * 保存云存储配置信息
     *
     * @param config 云存储配置信息
     * @param id id
     * @return 受影响的行数
     */
    @Override
    public Integer saveConfig(CloudConfigDTO config, Long id) {
        MinioConfigEntity minioConfigEntity = new MinioConfigEntity(
                id,
                config.getMinioEndPoint(),
                config.getMinioAccessKey(),
                config.getMinioSecretKey(),
                config.getMinioPublicBucketName(),
                config.getMinioPrivateBucketName(),
                config.getMinioUserInfoBucketName()
        );
        return cloudMapper.insertMinioConfig(minioConfigEntity);
    }

    /**
     * 删除云存储配置信息
     *
     * @param id id
     * @return 删除的行数
     */
    @Override
    public Integer deleteConfig(Long id) {
        return cloudMapper.deleteMinioConfigById(id);
    }

    /**
     * 获取云存储配置信息
     *
     * @return 云存储配置信息
     */
    @Override
    public List<?> listConfig() {
        return cloudMapper.listMinioConfig();
    }

    /**
     * 获取云存储配置信息
     *
     * @param id id
     * @return 云存储配置信息
     */
    @Override
    public MinioConfigEntity getConfig(Long id) {
        return cloudMapper.getMinioConfigById(id);
    }
}