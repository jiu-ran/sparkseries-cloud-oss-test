package com.sparkseries.module.oss.provider.service.impl.minio.factory;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * COS客户端对象工厂
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

    @Override
    public MinioClient create() {
        MinioClient minio = MinioClient.builder().
                endpoint(endpoint).
                credentials(accessKey, secretKey).
                build();
        log.info("Minio 客户端初始化完成");
        return minio;
    }


    @Override
    public PooledObject<MinioClient> wrap(MinioClient client) {
        return new DefaultPooledObject<>(client);
    }

    @Override
    public void destroyObject(PooledObject<MinioClient> p) throws Exception {
        // 关闭COS客户端
        p.getObject().close();
    }

    @Override
    public boolean validateObject(PooledObject<MinioClient> p) {
        return true;
    }
}