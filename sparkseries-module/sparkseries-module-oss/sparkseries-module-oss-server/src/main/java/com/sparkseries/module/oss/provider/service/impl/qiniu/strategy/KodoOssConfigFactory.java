package com.sparkseries.module.oss.provider.service.impl.qiniu.strategy;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.cloud.dao.CloudConfigMapper;
import com.sparkseries.module.oss.cloud.dto.CloudConfigDTO;
import com.sparkseries.module.oss.cloud.entity.KodoConfigEntity;
import com.sparkseries.module.oss.provider.service.base.factory.OssConfigFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kodo配置保存策略
 */
@Component
@RequiredArgsConstructor
public class KodoOssConfigFactory implements OssConfigFactory {

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