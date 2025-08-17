package com.sparkseries.module.oss.provider.qiniu.factory;

import com.qiniu.util.Auth;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * KODO客户端对象工厂
 */
@Slf4j
public class KodoClientFactory extends BasePooledObjectFactory<Auth> {

    private final String secretKey;
    private final String accessKey;


    public KodoClientFactory(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    @Override
    public Auth create() {
        Auth auth = Auth.create(accessKey, secretKey);
        log.info("KODO 客户端初始化完成");
        return auth;
    }

    /**
     * 包装对象
     */
    @Override
    public PooledObject<Auth> wrap(Auth auth) {
        return new DefaultPooledObject<>(auth);
    }

}
