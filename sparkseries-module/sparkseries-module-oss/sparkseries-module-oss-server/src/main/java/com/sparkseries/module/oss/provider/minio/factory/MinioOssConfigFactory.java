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
 * Minio配置保存策略
 */
@Component
@RequiredArgsConstructor
public class MinioOssConfigFactory implements OssConfigFactory {
    
    private final CloudConfigMapper cloudMapper;
    
    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.MINIO;
    }
    
    @Override
    public Integer saveConfig(CloudConfigDTO config, Long id) {
        MinioConfigEntity minioConfigEntity = new MinioConfigEntity(
            id, 
            config.getMinioEndPoint(),
            config.getMinioAccessKey(),
            config.getMinioSecretKey(), 
            config.getMinioBucketName()
        );
        return cloudMapper.insertMinioConfig(minioConfigEntity);
    }

    @Override
    public Integer deleteConfig(Long id) {
        return cloudMapper.deleteMinioConfigById(id);
    }

    @Override
    public List<?> listConfig() {
        return cloudMapper.listMinioConfig();
    }
}