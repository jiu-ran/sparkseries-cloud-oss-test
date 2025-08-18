package com.sparkseries.module.oss.switching;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.cloud.entity.CloudActiveEntity;
import com.sparkseries.module.oss.common.api.provider.factory.OssServiceFactory;
import com.sparkseries.module.oss.common.api.provider.service.OssService;
import com.sparkseries.module.oss.common.exception.OssException;
import com.sparkseries.module.oss.common.util.SpringBeanUtil;
import com.sparkseries.module.oss.storage.dao.StorageMapper;
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

import static com.sparkeries.enums.StorageTypeEnum.LOCAL;

/**
 * 存储策略提供者
 */
@Slf4j
@Component
public class DynamicStorageSwitchService {

    private final StorageMapper storageMapper;
    private final Map<StorageTypeEnum, OssService> OssMap = new ConcurrentHashMap<>();
    private final AtomicReference<OssService> currentStrategy = new AtomicReference<>();
    private final SpringBeanUtil springBeanUtil;
    private final Map<StorageTypeEnum, OssServiceFactory> factoryMap;


    public DynamicStorageSwitchService(List<OssService> ossServices, StorageMapper storageMapper,
                                       SpringBeanUtil springBeanUtil, List<OssServiceFactory> factories) {
        this.storageMapper = storageMapper;

        OssMap.putAll(ossServices.stream().collect(Collectors.toMap(OssService::getStorageType, value -> value)));

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
            try {
                changeOssService(type, id);
            } catch (Exception e) {
                log.warn("云存储服务启动失败，自动切换到本地存储服务 错误信息: {}", e.getMessage());
                storageMapper.deleteCloudActive();
                setCurrentStrategy(LOCAL);
                log.info("已自动切换到本地存储服务");
            }

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
            throw new OssException("存储服务没有正常启动,无法进行正常操作");
        }
        return ossService;
    }

    /**
     * 动态切换存储策略
     *
     * @param storageEnum 存储服务类型
     */
    public void setCurrentStrategy(StorageTypeEnum storageEnum) {
        currentStrategy.set(OssMap.get(storageEnum));
    }

    /**
     * 获取当前存储服务类型
     *
     * @return 当前存储服务类型枚举
     */
    public StorageTypeEnum getCurrentStorageEnum() {
        return getCurrentStrategy().getStorageType();
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
                throw new OssException("没有找到类型为 " + storageEnum.name() + " 的存储服务工厂");
            }
            try {
                OssService ossService = factory.createService(id);
                springBeanUtil.registerSingleton(storageEnum.getKey(), ossService);
                OssMap.put(storageEnum, ossService);
                setCurrentStrategy(storageEnum);
                log.info("启动存储服务成功,当前存储服务为 {}", storageEnum.name());
            } catch (Exception e) {
                log.error("存储服务切换失败 错误信息:{}", e.getMessage());
                throw new OssException("存储服务切换失败");
            }

        }
    }

}

