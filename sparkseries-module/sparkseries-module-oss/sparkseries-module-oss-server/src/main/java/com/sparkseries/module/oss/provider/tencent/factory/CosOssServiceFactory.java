package com.sparkseries.module.oss.provider.tencent.factory;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkeries.enums.VisibilityEnum;
import com.sparkseries.module.oss.cloud.dao.CloudConfigMapper;
import com.sparkseries.module.oss.cloud.entity.CosConfigEntity;
import com.sparkseries.module.oss.common.api.provider.factory.OssServiceFactory;
import com.sparkseries.module.oss.common.api.provider.service.OssService;
import com.sparkseries.module.oss.common.config.PoolConfig;
import com.sparkseries.module.oss.common.exception.OssException;
import com.sparkseries.module.oss.file.dao.MetadataMapper;
import com.sparkseries.module.oss.provider.tencent.connection.CosValidConnectServiceImpl;
import com.sparkseries.module.oss.provider.tencent.oss.CosOssServiceImpl;
import com.sparkseries.module.oss.provider.tencent.pool.CosClientPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * COS 存储服务工厂
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CosOssServiceFactory implements OssServiceFactory {

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
        return StorageTypeEnum.COS;
    }
    /**
     * 创建 COS 存储服务
     *
     * @param id id
     * @return 文件存储服务接口
     */
    @Override
    public OssService createService(Long id) {
        CosConfigEntity cos = cloudConfigMapper.getCosConfigById(id);
        Map<VisibilityEnum, String> map = new HashMap<>(3);
        map.put(VisibilityEnum.PUBLIC, cos.getPublicBucketName());
        map.put(VisibilityEnum.PRIVATE, cos.getPrivateBucketName());
        map.put(VisibilityEnum.USER_AVATAR, cos.getUserInfoBucketName());
        if (ObjectUtils.isEmpty(cos)) {
            throw new OssException("COS 该配置文件不存在 请先保存再进行切换");
        }
        if (!new CosValidConnectServiceImpl().connectTest(cos.getSecretId(), cos.getSecretKey(), map.get(VisibilityEnum.PUBLIC), cos.getRegion())) {
            log.warn("COS的桶 {} 测试失败", VisibilityEnum.PUBLIC);
            throw new OssException("保存的COS存储配置失效了请重新保存");
        }

        if (!new CosValidConnectServiceImpl().connectTest(cos.getSecretId(), cos.getSecretKey(), map.get(VisibilityEnum.PRIVATE), cos.getRegion())) {
            log.warn("COS的桶 {} 测试失败", VisibilityEnum.PRIVATE);
            throw new OssException("保存的COS存储配置失效了请重新保存");
        }

        if (!new CosValidConnectServiceImpl().connectTest(cos.getSecretId(), cos.getSecretKey(), map.get(VisibilityEnum.USER_AVATAR), cos.getRegion())) {
            log.warn("COS的桶 {} 测试失败", VisibilityEnum.USER_AVATAR);
            throw new OssException("保存的COS存储配置失效了请重新保存");
        }
        CosClientPool cosClientPool = new CosClientPool(cos.getSecretId(), cos.getSecretKey(), cos.getRegion(), poolConfig);
        return new CosOssServiceImpl(cosClientPool, map, metadataMapper);
    }
}