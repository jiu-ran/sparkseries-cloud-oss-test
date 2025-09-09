package com.sparkseries.module.oss.cloud.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kodo 配置信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "KODO 云服务配置信息")
public class KodoConfigEntity {
    @Schema(description = "KODO 云服务 id")
    public long id;
    @Schema(description = "KODO 云服务 accessKey")
    public String accessKey;
    @Schema(description = "KODO 云服务 secretKey")
    public String secretKey;
    @Schema(description = "KODO 云服务公共桶名")
    public String publicBucketName;
    @Schema(description = "KODO 云服务私有桶名")
    public String privateBucketName;
    @Schema(description = "KODO 云服务用户信息桶名")
    public String userInfoBucketName;
}