package com.sparkseries.module.cloudconfig.factory.config;

import com.sparkseries.common.enums.StorageTypeEnum;
import com.sparkseries.module.cloudconfig.dao.CloudConfigMapper;
import com.sparkseries.module.cloudconfig.dto.CloudConfigDTO;
import com.sparkseries.module.cloudconfig.entity.KodoConfigEntity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kodo配置保存策略
 */
@Component
@RequiredArgsConstructor
public class KodoConfigSaveStrategy implements ConfigSaveStrategy {
    
    private final CloudConfigMapper cloudMapper;
    
    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.KODO;
    }
    
    @Override
    public Integer saveConfig(CloudConfigDTO config, Long id) {
        KodoConfigEntity kodoConfigEntity = new KodoConfigEntity(
            id, 
            config.getKodoAccessKey(),
            config.getKodoSecretKey(),
            config.getKodoBucketName()
        );
        return cloudMapper.insertKodoConfig(kodoConfigEntity);
    }

    @Override
    public Integer deleteConfig(Long id) {
        return cloudMapper.deleteKodoConfigById(id);
    }

    @Override
    public List<?> listConfig() {
        return cloudMapper.listKodoConfig();
    }
}