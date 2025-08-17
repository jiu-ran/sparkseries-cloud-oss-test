package com.sparkseries.module.oss.common.api.provider.service;


import com.sparkseries.module.oss.cloud.dto.CloudConfigDTO;

/**
 * 云服务连接验证接口
 */
public interface ValidConnectService {

    /**
     * 验证云存储配置连接
     *
     * @param config 云存储配置DTO
     * @return 连接是否有效
     */
    boolean validConnect(CloudConfigDTO config);
}
