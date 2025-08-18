package com.sparkseries.module.oss.provider.aliyun.pool;

import com.aliyun.oss.OSS;
import com.sparkseries.module.oss.common.config.PoolConfig;
import com.sparkseries.module.oss.common.exception.OssException;
import com.sparkseries.module.oss.provider.aliyun.factory.OssClientFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * OSS客户端连接池
 */
public class OssClientPool {

    private final GenericObjectPool<OSS> pool;

    public OssClientPool(String endpoint, String accessKeyId,
                         String accessKeySecret, String region, PoolConfig poolConfig) {
        // 创建对象池配置
        GenericObjectPoolConfig<OSS> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(poolConfig.getMaxTotal());
        config.setMinIdle(poolConfig.getMinIdle());
        config.setMaxIdle(poolConfig.getMaxIdle());
        config.setTestOnBorrow(poolConfig.isTestOnBorrow());
        config.setTestOnReturn(poolConfig.isTestOnReturn());

        // 创建对象池
        this.pool = new GenericObjectPool<>(
                new OssClientFactory(endpoint, accessKeyId, accessKeySecret, region),
                config
        );
    }

    /**
     * 获取OSS客户端
     *
     * @return OSS客户端
     */
    public OSS getClient() {
        try {
            return pool.borrowObject();
        } catch (Exception e) {
            throw new OssException("获取OSS客户端失败", e);
        }
    }

    /**
     * 归还OSS客户端
     *
     * @param client OSS客户端
     */
    public void returnClient(OSS client) {
        if (client != null) {
            pool.returnObject(client);
        }
    }

}