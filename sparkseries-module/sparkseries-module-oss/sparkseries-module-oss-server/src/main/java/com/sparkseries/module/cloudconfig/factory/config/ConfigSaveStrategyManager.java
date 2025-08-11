package com.sparkseries.module.cloudconfig.factory.config;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.common.util.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 配置保存策略管理器
 */
@Slf4j
@Component
public class ConfigSaveStrategyManager {

    private final Map<StorageTypeEnum, ConfigSaveStrategy> strategyMap;

    /**
     * 构造函数，自动注入所有策略实现
     *
     * @param strategies 所有ConfigSaveStrategy实现类的列表
     */
    public ConfigSaveStrategyManager(List<ConfigSaveStrategy> strategies) {
        this.strategyMap = strategies.stream().collect(Collectors.toMap(ConfigSaveStrategy::getStorageType, Function.identity()));
        log.info("初始化配置保存策略管理器，支持的存储类型: {}", strategyMap.keySet());
    }

    /**
     * 根据存储类型获取保存策略
     *
     * @param storageType 存储类型
     * @return 配置保存策略
     */
    public ConfigSaveStrategy getStrategy(StorageTypeEnum storageType) {
        ConfigSaveStrategy strategy = strategyMap.get(storageType);
        if (strategy == null) {
            throw new BusinessException("不支持的存储类型: " + storageType);
        }
        return strategy;
    }

    /**
     * 根据类型值获取保存策略
     *
     * @param type 存储类型值
     * @return 配置保存策略
     */
    public ConfigSaveStrategy getStrategy(Integer type) {
        StorageTypeEnum storageType = StorageTypeEnum.getStorageEnum(type);
        return getStrategy(storageType);
    }
}