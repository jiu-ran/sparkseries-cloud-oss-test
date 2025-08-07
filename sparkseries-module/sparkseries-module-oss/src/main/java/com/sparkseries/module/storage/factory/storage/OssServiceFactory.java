package com.sparkseries.module.storage.factory.storage;


import com.sparkseries.common.enums.StorageTypeEnum;
import com.sparkseries.module.storage.service.oss.OssService;

/**
 * 存储服务创建工厂接口
 */
public interface OssServiceFactory {
    /**
     * 获取该工厂支持的存储类型
     * @return StorageTypeEnum
     */
    StorageTypeEnum getStorageType();

    /**
     * 根据配置ID创建 OssService 实例
     * @param id 配置ID
     * @return OssService 实例
     */
    OssService createService(Long id);
}