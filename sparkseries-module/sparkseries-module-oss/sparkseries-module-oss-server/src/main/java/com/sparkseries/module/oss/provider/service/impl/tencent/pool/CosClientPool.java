package com.sparkseries.module.oss.provider.service.impl.tencent.pool;

import com.qcloud.cos.COSClient;
import com.sparkseries.module.oss.common.config.PoolConfig;
import com.sparkseries.module.oss.provider.service.impl.tencent.factory.CosClientFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;


/**
 * 腾讯云COS对象存储连接池
 */
public class CosClientPool {

    private final GenericObjectPool<COSClient> pool;

    public CosClientPool(String secretId, String secretKey,
                         String region, String bucketName, PoolConfig poolConfig) {

        GenericObjectPoolConfig<COSClient> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(poolConfig.getMaxTotal());
        config.setMinIdle(poolConfig.getMinIdle());
        config.setMaxIdle(poolConfig.getMaxIdle());
        config.setTestOnBorrow(poolConfig.isTestOnBorrow());
        config.setTestOnReturn(poolConfig.isTestOnReturn());
        // 创建对象池
        this.pool = new GenericObjectPool<>(
                new CosClientFactory(secretId, secretKey, region, bucketName),
                config
        );
    }


    public COSClient getClient() throws Exception {
        return pool.borrowObject();
    }


    public void returnClient(COSClient client) {
        if (client != null) {
            pool.returnObject(client);
        }
    }


    public void close() {
        pool.close();
    }
}