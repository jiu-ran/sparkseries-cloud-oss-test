package com.sparkseries.module.oss.provider.qiniu.factory;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkeries.enums.VisibilityEnum;
import com.sparkseries.module.oss.cloud.dao.CloudConfigMapper;
import com.sparkseries.module.oss.cloud.entity.KodoConfigEntity;
import com.sparkseries.module.oss.common.api.provider.factory.OssServiceFactory;
import com.sparkseries.module.oss.common.api.provider.service.OssService;
import com.sparkseries.module.oss.common.config.PoolConfig;
import com.sparkseries.module.oss.common.exception.OssException;
import com.sparkseries.module.oss.file.dao.MetadataMapper;
import com.sparkseries.module.oss.provider.qiniu.connection.KodoValidConnectServiceImpl;
import com.sparkseries.module.oss.provider.qiniu.oss.KodoOssServiceImpl;
import com.sparkseries.module.oss.provider.qiniu.pool.KodoClientPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.sparkeries.enums.VisibilityEnum.*;

/**
 * KODO 存储服务工厂
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KodoOssServiceFactory implements OssServiceFactory {

    private final CloudConfigMapper cloudConfigMapper;
    private final PoolConfig poolConfig;
    private final MetadataMapper metadataMapper;

    /**
     * 获取存储类型
     *
     * @return 存储类型枚举
     */
    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.KODO;
    }
    /**
     * 创建 KODO 存储服务
     *
     * @param id id
     * @return 文件存储服务接口
     */
    @Override
    public OssService createService(Long id) {
        KodoConfigEntity kodo = cloudConfigMapper.getKodoConfigById(id);
        if (ObjectUtils.isEmpty(kodo)) {
            throw new OssException("KODO 该配置文件不存在 请先保存再进行切换");
        }
        Map<VisibilityEnum, String> map = new HashMap<>(3);
        map.put(PUBLIC, kodo.getPublicBucketName());
        map.put(PRIVATE, kodo.getPrivateBucketName());
        map.put(USER_INFO, kodo.getUserInfoBucketName());
        if (!new KodoValidConnectServiceImpl().connectTest(kodo.getAccessKey(), kodo.getSecretKey(), map.get(PUBLIC))) {
            log.warn("KODO的桶 {} 测试失败", PUBLIC);
            throw new OssException("保存的KODO存储配置失效了请重新保存");
        }
        if (!new KodoValidConnectServiceImpl().connectTest(kodo.getAccessKey(), kodo.getSecretKey(), map.get(PRIVATE))) {
            log.warn("KODO的桶 {} 测试失败", PRIVATE);
            throw new OssException("保存的KODO存储配置失效了请重新保存");
        }
        if (!new KodoValidConnectServiceImpl().connectTest(kodo.getAccessKey(), kodo.getSecretKey(), map.get(USER_INFO))) {
            log.warn("KODO的桶 {} 测试失败", USER_INFO);
            throw new OssException("保存的KODO存储配置失效了请重新保存");
        }
        KodoClientPool kodoClientPool = new KodoClientPool(kodo.getAccessKey(), kodo.getSecretKey(), poolConfig);

        return new KodoOssServiceImpl(kodoClientPool, map, metadataMapper);
    }
}
