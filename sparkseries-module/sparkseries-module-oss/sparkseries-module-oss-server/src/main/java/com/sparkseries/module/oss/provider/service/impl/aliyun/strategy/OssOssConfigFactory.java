package com.sparkseries.module.oss.provider.service.impl.aliyun.strategy;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.cloud.dao.CloudConfigMapper;
import com.sparkseries.module.oss.cloud.dto.CloudConfigDTO;
import com.sparkseries.module.oss.cloud.entity.OssConfigEntity;
import com.sparkseries.module.oss.provider.service.base.factory.OssConfigFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OSS配置保存策略
 */
@Component
@RequiredArgsConstructor
public class OssOssConfigFactory implements OssConfigFactory {

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