package com.sparkseries.module.cloudconfig.service.cloudconfig;


import com.sparkseries.common.util.entity.Result;
import com.sparkseries.module.cloudconfig.dto.CloudConfigDTO;

import java.util.List;

public interface CloudConfigService {

    /**
     * 验证云存储配置的连接性
     *
     * @param config 待验证的云存储配置DTO
     * @return 包含操作结果的Result对象
     */
    boolean validCloudConfig(CloudConfigDTO config);

    /**
     * 保存云存储配置信息
     *
     * @param config 云存储配置信息
     * @return 包含操作结果的Result对象
     */
    Result<?> saveCloudConfig(CloudConfigDTO config);

    /**
     * 验证云存储配置的连接性并保存
     *
     * @param config 云存储配置信息
     * @return 包含操作结果的Result对象
     */
    Result<?> validAndSave(CloudConfigDTO config);

    /**
     * 删除指定的云存储配置
     *
     * @param type 配置类型
     * @param id   配置ID
     * @return 包含操作结果的Result对象
     */
    Result<?> deleteCloudConfig(int type, Long id);

    /**
     * 列出指定类型的所有云存储配置
     *
     * @param type 配置类型
     * @return 包含云存储配置列表的Result对象
     */
    Result<List<?>> listCloudConfig(int type);


}
