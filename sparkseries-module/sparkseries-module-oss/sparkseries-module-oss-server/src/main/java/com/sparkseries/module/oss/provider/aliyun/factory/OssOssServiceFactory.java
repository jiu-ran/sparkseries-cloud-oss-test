package com.sparkseries.module.oss.provider.aliyun.factory;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkeries.enums.VisibilityEnum;
import com.sparkseries.module.oss.cloud.dao.CloudConfigMapper;
import com.sparkseries.module.oss.cloud.entity.OssConfigEntity;
import com.sparkseries.module.oss.common.api.provider.factory.OssServiceFactory;
import com.sparkseries.module.oss.common.api.provider.service.OssService;
import com.sparkseries.module.oss.common.config.PoolConfig;
import com.sparkseries.module.oss.common.exception.OssException;
import com.sparkseries.module.oss.file.dao.MetadataMapper;
import com.sparkseries.module.oss.provider.aliyun.connection.OssValidConnectServiceImpl;
import com.sparkseries.module.oss.provider.aliyun.oss.OssOssServiceImpl;
import com.sparkseries.module.oss.provider.aliyun.pool.OssClientPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.sparkeries.enums.VisibilityEnum.*;

/**
 * OSS 存储服务工厂
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OssOssServiceFactory implements OssServiceFactory {

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
        return StorageTypeEnum.OSS;
    }

    /**
     * 创建 OSS 存储服务
     *
     * @param id id
     * @return 文件存储服务接口
     */
    @Override
    public OssService createService(Long id) {
        OssConfigEntity oss = cloudConfigMapper.getOssConfigById(id);
        Map<VisibilityEnum, String> map = new HashMap<>(3);
        map.put(PUBLIC, oss.getPublicBucketName());
        map.put(PRIVATE, oss.getPrivateBucketName());
        map.put(USER_AVATAR, oss.getUserInfoBucketName());
        if (ObjectUtils.isEmpty(oss)) {
            log.error("OSS 配置信息错误 OSS 存储服务初始化失败 错误信息:{}", "请检查OSS配置信息");
            throw new OssException("OSS 该配置文件不存在 请先保存再进行切换");
        }

        if (!new OssValidConnectServiceImpl().connectTest(oss.getEndpoint(), oss.getAccessKeyId(), oss.getAccessKeySecret(), map.get(PUBLIC), oss.getRegion())) {
            log.warn("OSS的桶 {} 测试失败", PUBLIC);
            throw new OssException("保存的OSS存储配置失效了请重新保存");
        }
        if (!new OssValidConnectServiceImpl().connectTest(oss.getEndpoint(), oss.getAccessKeyId(), oss.getAccessKeySecret(), map.get(PRIVATE), oss.getRegion())) {
            log.warn("OSS的桶 {} 测试失败", PRIVATE);
            throw new OssException("保存的OSS存储配置失效了请重新保存");
        }
        if (!new OssValidConnectServiceImpl().connectTest(oss.getEndpoint(), oss.getAccessKeyId(), oss.getAccessKeySecret(), map.get(USER_AVATAR), oss.getRegion())) {
            log.warn("OSS的桶 {} 测试失败", USER_AVATAR);
            throw new OssException("保存的OSS存储配置失效了请重新保存");
        }

        OssClientPool ossClientPool = new OssClientPool(oss.getEndpoint(), oss.getAccessKeyId(), oss.getAccessKeySecret(), oss.getRegion(), poolConfig);
        return new OssOssServiceImpl(ossClientPool, map, metadataMapper);
    }
}