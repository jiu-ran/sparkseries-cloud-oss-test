package com.sparkseries.module.cloudconfig.entity;

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
	public String bucketName;
	public String region;
}