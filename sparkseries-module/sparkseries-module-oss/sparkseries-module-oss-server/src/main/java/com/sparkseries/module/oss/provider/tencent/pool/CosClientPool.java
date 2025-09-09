package com.sparkseries.module.oss.provider.tencent.pool;

import com.qcloud.cos.COSClient;
import com.sparkseries.module.oss.common.config.PoolConfig;
import com.sparkseries.module.oss.common.exception.OssException;
import com.sparkseries.module.oss.provider.tencent.factory.CosClientFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;


/**
 * COS 客户端连接池
 */
public class CosClientPool {

    private final GenericObjectPool<COSClient> pool;

    public CosClientPool(String secretId, String secretKey,
                         String region, PoolConfig poolConfig) {

        GenericObjectPoolConfig<COSClient> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(poolConfig.getMaxTotal());
        config.setMinIdle(poolConfig.getMinIdle());
        config.setMaxIdle(poolConfig.getMaxIdle());
        config.setTestOnBorrow(poolConfig.isTestOnBorrow());
        config.setTestOnReturn(poolConfig.isTestOnReturn());
        // 创建对象池
        this.pool = new GenericObjectPool<>(
                new CosClientFactory(secretId, secretKey, region), config
        );
    }

    /**
     * 获取 COS 客户端
     *
     * @return COS 客户端
     */
    public COSClient getClient() {
        try {
            return pool.borrowObject();
        } catch (Exception e) {
            throw new OssException("获取 COS 客户端失败", e);
        }

    }

    /**
     * 归还 COS 客户端
     *
     * @param client COS 客户端
     */
    public void returnClient(COSClient client) {
        if (client != null) {
            pool.returnObject(client);
        }
    }

}