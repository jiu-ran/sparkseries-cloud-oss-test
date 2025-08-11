package com.sparkseries.module.cloudconfig.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OSS配置信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OssConfigEntity {
    public long id;
    public String endpoint;
    public String accessKeyId;
    public String accessKeySecret;
    public String bucketName;
    public String region;
}