package com.sparkseries.module.oss.storage.service;

import com.sparkseries.common.util.entity.Result;

public interface StorageService {

    /**
     * 更改当前激活的云存储服务
     *
     * @param type 服务类型
     * @param id   服务ID
     * @return 包含操作结果的Result对象
     */
    Result<?> changeService(int type, Long id);


    /**
      * 获取当前激活的云存储服务
     *
     * @return 包含激活云存储配置的Result对象
     */
    Result<String> getActiveStorageInfo();
}
