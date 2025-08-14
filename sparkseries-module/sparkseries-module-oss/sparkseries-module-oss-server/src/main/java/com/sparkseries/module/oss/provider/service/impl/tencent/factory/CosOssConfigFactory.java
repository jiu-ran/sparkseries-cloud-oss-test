package com.sparkseries.module.oss.provider.service.impl.tencent.factory;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.cloud.dao.CloudConfigMapper;
import com.sparkseries.module.oss.cloud.dto.CloudConfigDTO;
import com.sparkseries.module.oss.cloud.entity.CosConfigEntity;
import com.sparkseries.module.oss.provider.service.base.factory.OssConfigFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * COS配置保存策略
 */
@Component
@RequiredArgsConstructor
public class CosOssConfigFactory implements OssConfigFactory {

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