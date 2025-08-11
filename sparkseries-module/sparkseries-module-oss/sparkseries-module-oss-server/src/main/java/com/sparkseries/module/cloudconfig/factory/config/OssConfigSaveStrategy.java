package com.sparkseries.module.cloudconfig.factory.config;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.cloudconfig.dao.CloudConfigMapper;
import com.sparkseries.module.cloudconfig.dto.CloudConfigDTO;
import com.sparkseries.module.cloudconfig.entity.OssConfigEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OSS配置保存策略
 */
@Component
@RequiredArgsConstructor
public class OssConfigSaveStrategy implements ConfigSaveStrategy {

    private final CloudConfigMapper cloudMapper;

    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.OSS;
    }

    @Override
    public Integer saveConfig(CloudConfigDTO config, Long id) {
        OssConfigEntity ossConfigEntity = new OssConfigEntity(
                id,
                config.getOssEndPoint(),
                config.getOssAccessKeyId(),
                config.getOssAccessKeySecret(),
                config.getOssBucketName(),
                config.getOssRegion()
        );
        return cloudMapper.insertOssConfig(ossConfigEntity);
    }

    @Override
    public Integer deleteConfig(Long id) {
        return cloudMapper.deleteOssConfigById(id);
    }

    @Override
    public List<?> listConfig() {
        return cloudMapper.listOssConfig();
    }
}