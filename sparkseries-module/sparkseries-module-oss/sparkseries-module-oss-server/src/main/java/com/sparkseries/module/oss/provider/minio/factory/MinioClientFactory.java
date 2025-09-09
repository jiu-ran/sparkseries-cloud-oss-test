package com.sparkseries.module.oss.provider.minio.factory;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * COS 客户端对象工厂
 */
@Slf4j
public class MinioClientFactory extends BasePooledObjectFactory<MinioClient> {

    private final String endpoint;
    private final String accessKey;
    private final String secretKey;

    public MinioClientFactory(String endpoint, String accessKey, String secretKey) {
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    /**
     * 创建客户端
     *
     * @return Minio 客户端
     */
    @Override
    public MinioClient create() {
        MinioClient minio = MinioClient.builder().
                endpoint(endpoint).
                credentials(accessKey, secretKey).
                build();
        log.info("Minio 客户端初始化完成");
        return minio;
    }

    /**
     * 实现包装提供的实例
     */
    @Override
    public PooledObject<MinioClient> wrap(MinioClient client) {
        return new DefaultPooledObject<>(client);
    }
}