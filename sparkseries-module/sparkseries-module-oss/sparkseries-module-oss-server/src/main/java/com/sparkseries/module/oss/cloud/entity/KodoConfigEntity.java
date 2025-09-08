package com.sparkseries.module.oss.cloud.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kodo配置信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KodoConfigEntity {
	public long id;
	public String accessKey;
	public String secretKey;
    public String publicBucketName;
    public String privateBucketName;
    public String userInfoBucketName;
}