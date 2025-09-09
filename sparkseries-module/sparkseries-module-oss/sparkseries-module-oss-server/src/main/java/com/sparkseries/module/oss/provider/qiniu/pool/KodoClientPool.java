package com.sparkseries.module.oss.provider.qiniu.pool;

import com.qiniu.util.Auth;
import com.sparkseries.module.oss.common.config.PoolConfig;
import com.sparkseries.module.oss.common.exception.OssException;
import com.sparkseries.module.oss.provider.qiniu.factory.KodoClientFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * KODO 客户端连接池
 */
public class KodoClientPool {

    private final GenericObjectPool<Auth> pool;

    public KodoClientPool(String accessKey, String secretKey, PoolConfig poolConfig) {

        GenericObjectPoolConfig<Auth> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(poolConfig.getMaxTotal());
        config.setMinIdle(poolConfig.getMinIdle());
        config.setMaxIdle(poolConfig.getMaxIdle());
        config.setTestOnBorrow(poolConfig.isTestOnBorrow());
        config.setTestOnReturn(poolConfig.isTestOnReturn());

        // 创建对象池
        this.pool = new GenericObjectPool<>(
                new KodoClientFactory(accessKey, secretKey),
                config
        );
    }

    /**
     * 获取 KODO 客户端
     *
     * @return KODO 客户端
     */
    public Auth getClient() {
        try {
            return pool.borrowObject();
        } catch (Exception e) {
            throw new OssException("获取 KODO 客户端失败", e);
        }

    }

    /**
     * 归还 KODO 客户端
     *
     * @param client KODO 客户端
     */
    public void returnClient(Auth client) {
        if (client != null) {
            pool.returnObject(client);
        }
    }

}