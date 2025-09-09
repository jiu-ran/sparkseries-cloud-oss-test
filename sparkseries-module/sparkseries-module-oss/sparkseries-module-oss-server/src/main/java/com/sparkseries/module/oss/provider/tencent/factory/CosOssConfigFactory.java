package com.sparkseries.module.oss.provider.tencent.factory;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.cloud.dao.CloudConfigMapper;
import com.sparkseries.module.oss.cloud.dto.CloudConfigDTO;
import com.sparkseries.module.oss.cloud.entity.CosConfigEntity;
import com.sparkseries.module.oss.common.api.provider.factory.OssConfigFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * COS 配置保存策略
 */
@Component
@RequiredArgsConstructor
public class CosOssConfigFactory implements OssConfigFactory {

    private final CloudConfigMapper cloudMapper;

    /**
     * 获取存储类型
     *
     * @return 存储类型枚举
     */
    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.COS;
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
        CosConfigEntity cosConfigEntity = new CosConfigEntity(
                id,
                config.getCosSecretId(),
                config.getCosSecretKey(),
                config.getCosPublicBucketName(),
                config.getCosPrivateBucketName(),
                config.getCosUserInfoBucketName(),
                config.getCosRegion()
        );
        return cloudMapper.insertCosConfig(cosConfigEntity);
    }

    /**
     * 删除云存储配置信息
     *
     * @param id id
     * @return 删除的行数
     */
    @Override
    public Integer deleteConfig(Long id) {

        return cloudMapper.deleteCosConfigById(id);
    }

    /**
     * 获取云存储配置信息
     *
     * @return 云存储配置信息
     */
    @Override
    public List<CosConfigEntity> listConfig() {

        return cloudMapper.listCosConfig();
    }

    /**
     * 获取云存储配置信息
     *
     * @param id id
     * @return 云存储配置信息
     */
    @Override
    public CosConfigEntity getConfig(Long id) {

        return cloudMapper.getCosConfigById(id);
    }

}