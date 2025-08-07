package com.sparkseries.module.cloudconfig.factory.valid.manager;

import com.sparkseries.common.enums.StorageTypeEnum;
import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.module.cloudconfig.factory.valid.ValidConnectServiceFactory;
import com.sparkseries.module.cloudconfig.service.connect.ValidConnectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 连接验证服务工厂管理器
 */
@Slf4j
@Component
public class ValidConnectServiceFactoryManager {

    private final Map<StorageTypeEnum, ValidConnectServiceFactory> factoryMap;

    /**
     * 构造函数，自动注入所有工厂实现
     *
     * @param factories 所有ValidConnectServiceFactory实现类的列表
     */
    public ValidConnectServiceFactoryManager(List<ValidConnectServiceFactory> factories) {
        this.factoryMap = factories.stream()
                .collect(Collectors.toMap(
                        ValidConnectServiceFactory::getStorageType,
                        Function.identity()
                ));
        log.info("初始化连接验证服务工厂管理器，支持的存储类型: {}",
                factoryMap.keySet());
    }

    /**
     * 根据存储类型获取连接验证服务
     *
     * @param storageType 存储类型
     * @return 连接验证服务
     */
    public ValidConnectService getValidConnectService(StorageTypeEnum storageType) {
        ValidConnectServiceFactory factory = factoryMap.get(storageType);
        if (factory == null) {
            throw new BusinessException("不支持的存储类型: " + storageType);
        }
        return factory.createValidConnectService();
    }

    /**
     * 根据类型值获取连接验证服务
     *
     * @param type 存储类型值
     * @return 连接验证服务
     */
    public ValidConnectService getValidConnectService(Integer type) {
        StorageTypeEnum storageType = StorageTypeEnum.getStorageEnum(type);
        return getValidConnectService(storageType);
    }
}