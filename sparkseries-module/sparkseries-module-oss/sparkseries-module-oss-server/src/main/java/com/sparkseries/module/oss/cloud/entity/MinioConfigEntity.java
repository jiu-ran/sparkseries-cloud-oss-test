package com.sparkseries.module.oss.cloud.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minio配置信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MinioConfigEntity {
	public long id;
	public String endpoint;
	public String accessKey;
	public String secretKey;
    public String publicBucketName;
    public String privateBucketName;
    public String userInfoBucketName;
}