package com.sparkseries.module.storage.pool;


import com.sparkseries.common.config.PoolConfig;
import com.sparkseries.module.cloudconfig.factory.client.MinioClientFactory;
import io.minio.MinioClient;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * MinioClient连接池
 */
public class  MinioClientPool {

    private final GenericObjectPool<MinioClient> pool;

    public MinioClientPool(String endpoint, String accessKey, String secretKey, PoolConfig poolConfig) {
        // 创建对象池配置
        GenericObjectPoolConfig<MinioClient> config = new GenericObjectPoolConfig<>();
        // 从 poolProperties 对象中获取配置
        config.setMaxTotal(poolConfig.getMaxTotal());
        config.setMinIdle(poolConfig.getMinIdle());
        config.setMaxIdle(poolConfig.getMaxIdle());
        config.setTestOnBorrow(poolConfig.isTestOnBorrow());
        config.setTestOnReturn(poolConfig.isTestOnReturn());

        // 创建对象池
        this.pool = new GenericObjectPool<>(
                new MinioClientFactory(endpoint, accessKey, secretKey),
                config
        );
    }

    // 获取客户端
    public MinioClient getClient() throws Exception {
        return pool.borrowObject();
    }

    // 归还客户端
    public void returnClient(MinioClient client) {
        if (client != null) {
            pool.returnObject(client);
        }
    }

    // 关闭连接池
    public void close() {
        pool.close();
    }
}