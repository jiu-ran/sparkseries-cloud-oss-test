package com.sparkseries.module.cloudconfig.factory.config;

import com.sparkseries.common.enums.StorageTypeEnum;
import com.sparkseries.module.cloudconfig.dao.CloudConfigMapper;
import com.sparkseries.module.cloudconfig.dto.CloudConfigDTO;
import com.sparkseries.module.cloudconfig.entity.MinioConfigEntity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Minio配置保存策略
 */
@Component
@RequiredArgsConstructor
public class MinioConfigSaveStrategy implements ConfigSaveStrategy {
    
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