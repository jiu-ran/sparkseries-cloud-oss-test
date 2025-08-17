package com.sparkseries.module.oss.provider.aliyun.factory;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * OSS客户端对象工厂
 */
@Slf4j
public class OssClientFactory extends BasePooledObjectFactory<OSS> {

    private final String endpoint;
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String region;

    public OssClientFactory(String endpoint, String accessKeyId, String accessKeySecret, String region) {
        this.endpoint = endpoint;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.region = region;
    }

    @Override
    public OSS create() {
        DefaultCredentialProvider credentialsProvider = new DefaultCredentialProvider
                (accessKeyId, accessKeySecret);
        // 配置OSS的签名算法
        ClientBuilderConfiguration config = new ClientBuilderConfiguration();
        config.setSignatureVersion(SignVersion.V4);
        OSS oss = OSSClientBuilder.create().endpoint(endpoint).
                credentialsProvider(credentialsProvider).clientConfiguration(config).region(region).build();
        log.info("OSS 客户端初始化完成");
        return oss;
    }

    /**
     * 包装对象
     */
    @Override
    public PooledObject<OSS> wrap(OSS oss) {
        return new DefaultPooledObject<>(oss);
    }
}
