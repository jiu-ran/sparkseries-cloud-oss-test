package com.sparkseries.module.cloudconfig.entity;

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
	public String bucketName;
}