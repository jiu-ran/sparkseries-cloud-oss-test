package com.sparkseries.module.oss.storage.service.impl;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.common.util.entity.Result;
import com.sparkseries.module.oss.storage.dao.StorageMapper;
import com.sparkseries.module.oss.switching.DynamicStorageSwitchService;
import com.sparkseries.module.oss.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final DynamicStorageSwitchService provider;
    private final StorageMapper storageMapper;


    /**
     * 切换当前活跃的存储服务
     *
     * @param type 存储类型
     * @param id   配置ID
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> changeService(int type, Long id) {

        provider.changeOssService(type, id);

        storageMapper.deleteCloudActive();

        Integer row = storageMapper.insertCloudActive(id, type);

        if (row <= 0) {
            return Result.error("切换失败");
        }

        return Result.ok("切换成功");

    }


    /**
     * 获取当前活跃的存储服务
     *
     * @return 当前活跃存储服务
     */
    @Override
    public Result<String> getActiveStorageInfo() {

        StorageTypeEnum currentStorage = provider.getCurrentStorageEnum();

        return Result.ok("当前使用的是" + currentStorage.getKey() + "存储服务");
    }
}
