package com.sparkseries.module.cloudconfig.factory.client;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import com.sparkseries.common.util.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * COS客户端对象工厂
 */
@Slf4j
public class CosClientFactory extends BasePooledObjectFactory<COSClient> {

    private final String secretId;
    private final String secretKey;
    private final String region;
    private final String bucketName;

    public CosClientFactory(String secretId, String secretKey, String region, String bucketName) {
        this.secretId = secretId;
        this.secretKey = secretKey;
        this.region = region;
        this.bucketName = bucketName;
    }

    @Override
    public COSClient create() {
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 2 设置 bucket 的地域, COS 地域的简称
        // clientConfig 中包含了设置 region, https, 超时, 代理等 set 方法, 使用可参见源码或者常见问题 Java SDK 部分。
        Region Region = new Region(region);
        ClientConfig clientConfig = new ClientConfig(Region);
        // 这里建议设置使用 https 协议
        // 从 5.6.54 版本开始，默认使用了 https
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 3 生成 cos 客户端。
        COSClient client = new COSClient(cred, clientConfig);
        log.info("COS 客户端初始化完成");
        return client;
    }

    /**
     * 包装对象
     */
    @Override
    public PooledObject<COSClient> wrap(COSClient client) {
        return new DefaultPooledObject<>(client);
    }

    /**
     * 销毁对象
     */
    @Override
    public void destroyObject(PooledObject<COSClient> p) {
        p.getObject().shutdown(); // 关闭COS客户端
    }

    /**
     * 验证对象是否有效
     */
    @Override
    public boolean validateObject(PooledObject<COSClient> p) {
        try {
            // 简单验证客户端是否有效
            COSClient client = p.getObject();
            client.getBucketLocation(bucketName); // 测试请求
            return true;
        } catch (Exception e) {
            throw new BusinessException("COS客户端连接失败");
        }
    }
}