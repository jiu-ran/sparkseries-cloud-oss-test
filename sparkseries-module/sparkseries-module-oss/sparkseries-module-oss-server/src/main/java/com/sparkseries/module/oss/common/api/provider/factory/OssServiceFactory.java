package com.sparkseries.module.oss.common.api.provider.factory;


import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.common.api.provider.service.OssService;

/**
 * 存储服务创建工厂接口
 */
public interface OssServiceFactory {
    /**
     * 获取该工厂支持的存储类型
     *
     * @return 存储类型枚举
     */
    StorageTypeEnum getStorageType();

    /**
     * 根据配置 id 创建存储服务实例
     *
     * @param id id
     * @return 存储实例
     */
    OssService createService(Long id);
}