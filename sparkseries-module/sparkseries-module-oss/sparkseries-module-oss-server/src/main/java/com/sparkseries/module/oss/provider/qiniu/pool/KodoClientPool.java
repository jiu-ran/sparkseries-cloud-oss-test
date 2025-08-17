package com.sparkseries.module.oss.provider.qiniu.pool;

import com.qiniu.util.Auth;

import com.sparkseries.module.oss.common.config.PoolConfig;
import com.sparkseries.module.oss.provider.qiniu.factory.KodoClientFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * 七牛云 KODO 客户端连接池
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

    // 获取客户端
    public Auth getClient() throws Exception {
        return pool.borrowObject();
    }

    // 归还客户端
    public void returnClient(Auth client) {
        if (client != null) {
            pool.returnObject(client);
        }
    }

    // 关闭连接池
    public void close() {
        pool.close();
    }
}