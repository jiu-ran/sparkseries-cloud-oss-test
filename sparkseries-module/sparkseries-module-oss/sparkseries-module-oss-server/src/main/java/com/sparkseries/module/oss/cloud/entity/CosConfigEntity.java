package com.sparkseries.module.oss.cloud.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * COS（对象存储服务）的配置信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CosConfigEntity {
	public long id;
	public String secretId;
	public String secretKey;
    public String publicBucketName;
    public String privateBucketName;
    public String userInfoBucketName;
	public String region;
}