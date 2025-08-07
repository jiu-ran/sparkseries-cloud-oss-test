package com.sparkseries.module.cloudconfig.factory.config;

import com.sparkseries.common.enums.StorageTypeEnum;
import com.sparkseries.module.cloudconfig.dto.CloudConfigDTO;

import java.util.List;

/**
 * 配置保存策略接口
 */
public interface ConfigSaveStrategy {

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

    Integer deleteConfig(Long id);


    List<?> listConfig();


}