package com.sparkseries.module.storage.provider;

import com.sparkseries.common.enums.StorageTypeEnum;
import com.sparkseries.common.util.SpringBeanUtil;
import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.module.cloudconfig.entity.CloudActiveEntity;
import com.sparkseries.module.storage.dao.StorageMapper;
import com.sparkseries.module.storage.factory.storage.OssServiceFactory;
import com.sparkseries.module.storage.service.oss.OssService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.sparkseries.common.enums.StorageTypeEnum.LOCAL;

/**
 * 存储策略提供者
 */
@Slf4j
@Component
public class StorageStrategyProvider {

    private final StorageMapper storageMapper;
    private final Map<String, OssService> strategyMap = new ConcurrentHashMap<>();
    private final AtomicReference<OssService> currentStrategy = new AtomicReference<>();
    private final SpringBeanUtil springBeanUtil;
    private final Map<StorageTypeEnum, OssServiceFactory> factoryMap;


    public StorageStrategyProvider(Map<String, OssService> strategyMap, StorageMapper storageMapper,
                                   SpringBeanUtil springBeanUtil, List<OssServiceFactory> factories) {
        this.storageMapper = storageMapper;
        this.strategyMap.putAll(strategyMap);
        this.springBeanUtil = springBeanUtil;
        this.factoryMap = factories.stream()
                .collect(Collectors.toMap(OssServiceFactory::getStorageType, Function.identity()));
    }

    @PostConstruct
    public void init() {
        CloudActiveEntity active = storageMapper.getCloudActive();
        if (ObjectUtils.isEmpty(active)) {
            setCurrentStrategy(LOCAL);
            log.info("当前存储服务为本地存储服务");
        } else {
            Long id = active.getId();
            int type = active.getType();
            changeOssService(type, id);
        }
    }

    /**
     * 获取当前的存储服务实例
     *
     * @return 当前激活的 CloudConfigService
     */
    public OssService getCurrentStrategy() {
        OssService ossService = this.currentStrategy.get();
        if (ossService == null) {
            log.error("存储服务没有正常启动,无法进行正常操作");
            throw new BusinessException("存储服务没有正常启动,无法进行正常操作");
        }
        return ossService;
    }

    /**
     * 动态切换存储策略
     *
     * @param storageEnum 存储服务类型
     */
    public void setCurrentStrategy(StorageTypeEnum storageEnum) {
        currentStrategy.set(strategyMap.get(storageEnum.getKey()));
    }

    /**
     * 获取当前存储服务类型
     *
     * @return 当前存储服务类型枚举
     */
    public StorageTypeEnum getCurrentStorageEnum() {
        return getCurrentStrategy().getCurrStorageType();
    }

    /**
     * 切换存储服务
     *
     * @param type 存储服务类型
     * @param id   存储服务ID
     */
    public void changeOssService(int type, Long id) {
        StorageTypeEnum storageEnum = StorageTypeEnum.getStorageEnum(type);
        if (storageEnum == LOCAL) {
            setCurrentStrategy(LOCAL);
        } else {
            OssServiceFactory factory = factoryMap.get(storageEnum);
            if (factory == null) {
                throw new BusinessException("没有找到类型为 " + storageEnum.name() + " 的存储服务工厂");
            }
            OssService ossService = factory.createService(id);
            springBeanUtil.registerSingleton(storageEnum.getKey(), ossService);
            strategyMap.put(storageEnum.getKey(), ossService);
            setCurrentStrategy(storageEnum);
            log.info("启动存储服务成功,当前存储服务为 {}", storageEnum.name());
        }
    }

}

