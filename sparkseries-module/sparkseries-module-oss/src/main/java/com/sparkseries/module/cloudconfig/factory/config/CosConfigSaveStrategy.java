package com.sparkseries.module.cloudconfig.factory.config;

import com.sparkseries.common.enums.StorageTypeEnum;
import com.sparkseries.module.cloudconfig.dao.CloudConfigMapper;
import com.sparkseries.module.cloudconfig.dto.CloudConfigDTO;
import com.sparkseries.module.cloudconfig.entity.CosConfigEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * COS配置保存策略
 */
@Component
@RequiredArgsConstructor
public class CosConfigSaveStrategy implements ConfigSaveStrategy {

    private final CloudConfigMapper cloudMapper;

    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.COS;
    }

    @Override
    public Integer saveConfig(CloudConfigDTO config, Long id) {
        CosConfigEntity cosConfigEntity = new CosConfigEntity(
                id,
                config.getCosSecretId(),
                config.getCosSecretKey(),
                config.getCosBucketName(),
                config.getCosRegion()
        );
        return cloudMapper.insertCosConfig(cosConfigEntity);
    }

    @Override
    public Integer deleteConfig(Long id) {

        return cloudMapper.deleteCosConfigById(id);
    }

    @Override
    public List<CosConfigEntity> listConfig() {

        return cloudMapper.listCosConfig();
    }


}