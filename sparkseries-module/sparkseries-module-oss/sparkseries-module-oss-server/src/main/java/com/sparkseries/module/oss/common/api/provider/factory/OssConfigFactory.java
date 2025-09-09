package com.sparkseries.module.oss.common.api.provider.factory;

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
     * @param config 云存储配置信息
     * @param id id
     * @return 影响的行数
     */
    Integer saveConfig(CloudConfigDTO config, Long id);

    /**
     * 删除配置
     *
     * @param id id
     * @return 影响的行数
     */
    Integer deleteConfig(Long id);

    /**
     * 获取所有配置
     *
     * @return 云配置信息列表
     */
    List<?> listConfig();

    /**
     * 获取配置
     *
     * @param id id
     * @return 配置信息
     */
    Object getConfig(Long id);

}