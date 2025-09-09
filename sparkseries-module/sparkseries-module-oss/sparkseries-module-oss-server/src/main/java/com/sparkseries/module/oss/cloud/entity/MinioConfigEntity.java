package com.sparkseries.module.oss.cloud.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minio 配置信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Minio 云服务配置信息")
public class MinioConfigEntity {
    @Schema(description = "Minio 云服务 id")
    public long id;
    @Schema(description = "Minio 云服务 endpoint")
    public String endpoint;
    @Schema(description = "Minio 云服务 accessKey")
    public String accessKey;
    @Schema(description = "Minio 云服务 secretKey")
    public String secretKey;
    @Schema(description = "Minio 云服务公共桶名")
    public String publicBucketName;
    @Schema(description = "Minio 云服务私有桶名")
    public String privateBucketName;
    @Schema(description = "Minio 云服务用户信息桶名")
    public String userInfoBucketName;
}