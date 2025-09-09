package com.sparkseries.module.oss.cloud.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OSS 配置信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "OSS 云服务配置信息")
public class OssConfigEntity {
    @Schema(description = "OSS 云服务 id")
    public long id;
    @Schema(description = "OSS 云服务 endpoint")
    public String endpoint;
    @Schema(description = "OSS 云服务 accessKey")
    public String accessKeyId;
    @Schema(description = "OSS 云服务 secretKey")
    public String accessKeySecret;
    @Schema(description = "OSS 云服务公共桶名")
    public String publicBucketName;
    @Schema(description = "OSS 云服务私有桶名")
    public String privateBucketName;
    @Schema(description = "OSS 云服务用户信息桶名")
    public String userInfoBucketName;
    @Schema(description = "OSS 云服务所属地区")
    public String region;
}