package com.sparkseries.module.oss.provider.service.base.factory;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.cloud.dto.CloudConfigDTO;

import java.util.List;

/**
 * 配置保存策略接口
 */
public interface OssConfigFactory {

    /**
     * 获取支持的存储类型
     *
     * @return 存储类型枚举
     */
    StorageTypeEnum getStorageType();

    /**
     * 保存配置
     *
     * @param config 云存储配置DTO
     * @param id     生成的ID
     * @return 影响的行数
     */
    Integer saveConfig(CloudConfigDTO config, Long id);

    /**
     * 删除配置
     *
     * @param id 配置ID
     * @return 影响的行数
     */
    Integer deleteConfig(Long id);

    /**
     * 获取所有配置
     *
     * @return 所有配置
     */
    List<?> listConfig();


}