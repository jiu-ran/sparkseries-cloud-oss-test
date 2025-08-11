package com.sparkseries.module.provider.aliyun.pool;

import com.aliyun.oss.OSS;
import com.sparkseries.common.config.PoolConfig;
import com.sparkseries.module.cloudconfig.factory.client.OssClientFactory;
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

    // 获取客户端
    public OSS getClient() throws Exception {
        return pool.borrowObject();
    }

    // 归还客户端
    public void returnClient(OSS client) {
        if (client != null) {
            pool.returnObject(client);
        }
    }

    // 关闭连接池
    public void close() {
        pool.close();
    }
}