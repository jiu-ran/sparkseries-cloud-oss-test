package com.sparkseries.common.util.config;

import com.sparkseries.common.util.tool.GeoIPParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * IP 解析器配置
 */
@Configuration
public class GeoIPConfig {
    private final String dbPath = "sparkseries-common/sparkseries-common-util/src/main/resources/GeoLite2-City.mmdb";

    @Bean
    public GeoIPParser geoIPService() throws Exception {
        // 处理 classpath 路径 (如 "classpath:GeoLite2-City.mmdb")
        if (dbPath.startsWith("classpath:")) {
            String classpathLocation = dbPath.replace("classpath:", "");
            Resource resource = new ClassPathResource(classpathLocation);
            return new GeoIPParser(resource.getFile().getAbsolutePath());
        } else {
            // 处理文件系统绝对路径
            return new GeoIPParser(dbPath);
        }
    }
}