package com.sparkseries.module.oss.cloud.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * COS 配置信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "COS 云服务配置信息")
public class CosConfigEntity {
    @Schema(description = "COS云服务 id")
    public long id;
    @Schema(description = "COS 云服务 secretId")
    public String secretId;
    @Schema(description = "COS 云服务 secretKey")
    public String secretKey;
    @Schema(description = "COS 云服务公共桶名")
    public String publicBucketName;
    @Schema(description = "COS 云服务私有桶名")
    public String privateBucketName;
    @Schema(description = "COS 云服务用户信息桶名")
    public String userInfoBucketName;
    @Schema(description = "COS 云服务所属地区")
    public String region;
}