package com.sparkseries.module.oss.common.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 连接池配置属性
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "pool.config")
public class PoolConfig {

    /**
     * 池中最大连接数
     */
    private int maxTotal = 10;

    /**
     * 池中最小空闲连接数
     */
    private int minIdle = 2;

    /**
     * 池中最大空闲连接数
     */
    private int maxIdle = 5;

    /**
     * 获取连接时是否测试连接的有效性
     */
    private boolean testOnBorrow = true;

    /**
     * 归还连接时是否测试连接的有效性
     */
    private boolean testOnReturn = true;
}