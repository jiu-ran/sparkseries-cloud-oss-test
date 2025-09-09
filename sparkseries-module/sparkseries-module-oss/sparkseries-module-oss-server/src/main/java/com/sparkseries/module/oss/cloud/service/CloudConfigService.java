package com.sparkseries.module.oss.cloud.service;


import com.sparkseries.common.util.entity.Result;
import com.sparkseries.module.oss.cloud.dto.CloudConfigDTO;

import java.util.List;

/**
 * 云存储配置管理
 */
public interface CloudConfigService {

    /**
     * 验证云存储配置的连接性
     *
     * @param config 待验证的云存储配置信息
     * @return 验证结果
     */
    boolean validCloudConfig(CloudConfigDTO config);

    /**
     * 保存云存储配置信息
     *
     * @param config 云存储配置信息
     * @return 默认响应类
     */
    Result<?> saveCloudConfig(CloudConfigDTO config);

    /**
     * 验证云存储配置的连接性并保存
     *
     * @param config 云存储配置信息
     * @return 默认响应类
     */
    Result<?> validAndSave(CloudConfigDTO config);

    /**
     * 删除指定的云存储配置
     *
     * @param type 配置类型
     * @param id 配置 ID
     * @return 默认响应类
     */
    Result<?> deleteCloudConfig(int type, Long id);

    /**
     * 列出指定类型的所有云存储配置
     *
     * @param type 配置类型
     * @return 默认响应类
     */
    Result<List<?>> listCloudConfig(int type);


}
