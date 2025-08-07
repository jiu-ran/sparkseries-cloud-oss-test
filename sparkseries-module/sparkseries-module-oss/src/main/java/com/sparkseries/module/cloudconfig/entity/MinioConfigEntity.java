package com.sparkseries.module.cloudconfig.entity;

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
	public String bucketName;
}