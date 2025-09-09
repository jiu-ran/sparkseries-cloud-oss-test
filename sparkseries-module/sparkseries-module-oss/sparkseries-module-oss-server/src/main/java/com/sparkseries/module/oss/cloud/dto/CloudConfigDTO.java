package com.sparkseries.module.oss.cloud.dto;


import com.sparkeries.enums.StorageTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.io.Serializable;
import java.util.Map;

/**
 * 云存储配置数据传输对象
 */
@Slf4j
@Data
@Schema(description = "云存储配置信息")
public class CloudConfigDTO implements Serializable {

    @Schema(description = "类型 1：阿里云  2：腾讯云  3:七牛云  4：Minio")
    private StorageTypeEnum typeEnum;
    @Schema(description = "阿里云OSS的endPoint")
    @NotBlank(message = "请输入你的阿里云OSS的endPoint", groups = OssGroup.class)
    private String ossEndPoint;
    @Schema(description = "阿里云OSS的accessKeyId")
    @NotBlank(message = "请输入你的阿里云OSS的accessKeyId", groups = OssGroup.class)
    private String ossAccessKeyId;
    @Schema(description = "阿里云OSS的accessKeySecret")
    @NotBlank(message = "请输入你的阿里云OSS的accessKeySecret", groups = OssGroup.class)
    private String ossAccessKeySecret;
    @Schema(description = "阿里云OSS的公共存储桶桶名")
    @NotBlank(message = "请输入你的阿里云OSS的公共存储桶桶名", groups = OssGroup.class)
    private String ossPublicBucketName;
    @Schema(description = "阿里云OSS的私有存储桶桶名")
    @NotBlank(message = "请输入你的阿里云OSS的私有存储桶桶名", groups = OssGroup.class)
    private String ossPrivateBucketName;
    @Schema(description = "阿里云OSS的用户信息存储桶桶名")
    @NotBlank(message = "请输入你的阿里云OSS的用户信息存储桶桶名", groups = OssGroup.class)
    private String ossUserInfoBucketName;
    @Schema(description = "阿里云OSS所属地区")
    @NotBlank(message = "请输入你的阿里云OSS所属地", groups = OssGroup.class)
    private String ossRegion;
    @Schema(description = "腾讯云COS的secretId")
    @NotBlank(message = "请输入你的腾讯云COS的secretId", groups = CosGroup.class)
    private String cosSecretId;
    @Schema(description = "腾讯云COS的secretKey")
    @NotBlank(message = "请输入你的腾讯云COS的secretKey", groups = CosGroup.class)
    private String cosSecretKey;

    @Schema(description = "腾讯云COS的公共存储桶桶名")
    @NotBlank(message = "请输入你的腾讯云COS的公共存储桶桶名", groups = CosGroup.class)
    private String cosPublicBucketName;
    @Schema(description = "腾讯云COS的私有存储桶桶名")
    @NotBlank(message = "请输入你的腾讯云COS的私有存储桶桶名", groups = CosGroup.class)
    private String cosPrivateBucketName;
    @Schema(description = "腾讯云COS的用户信息存储桶桶名")
    @NotBlank(message = "腾讯云COS的用户信息存储桶桶名", groups = CosGroup.class)
    private String cosUserInfoBucketName;

    @Schema(description = "腾讯云COS的所属地区")
    @NotBlank(message = "请输入你的腾讯云COS的所属地", groups = CosGroup.class)
    private String cosRegion;
    @Schema(description = "七牛KODO的accessKey")
    @NotBlank(message = "请输入你的七牛云KODO的accessKey", groups = KodoGroup.class)
    private String kodoAccessKey;
    @Schema(description = "七牛KODO的secretKey")
    @NotBlank(message = "请输入你的七牛云KODO的secretKey", groups = KodoGroup.class)
    private String kodoSecretKey;

    @Schema(description = "七牛存KODO的公共存储桶桶名")
    @NotBlank(message = "请输入你的七牛云KODO的公共存储桶桶名", groups = KodoGroup.class)
    private String kodoPublicBucketName;
    @Schema(description = "七牛KODO的私有存储桶桶名")
    @NotBlank(message = "请输入你的七牛云KODO的私有存储桶桶名", groups = KodoGroup.class)
    private String kodoPrivateBucketName;
    @Schema(description = "七牛KODO的用户信息存储桶桶名")
    @NotBlank(message = "请输入你的七牛云KODO的用户信息存储桶桶名", groups = KodoGroup.class)
    private String kodoUserInfoBucketName;

    @Schema(description = "Minio的endPoint")
    @NotBlank(message = "请输入你的Minio的endpoint", groups = MinioGroup.class)
    private String minioEndPoint;
    @Schema(description = "Minio的accessKey")
    @NotBlank(message = "请输入你的Minio的accessKey", groups = MinioGroup.class)
    private String minioAccessKey;
    @Schema(description = "Minio的secretKey")
    @NotBlank(message = "请输入你的Minio的secretKey", groups = MinioGroup.class)
    private String minioSecretKey;

    @Schema(description = "Minio的公共存储桶桶名")
    @NotBlank(message = "请输入你的Minio的公共存储桶桶名", groups = MinioGroup.class)
    private String minioPublicBucketName;
    @Schema(description = "Minio的私有存储桶桶名")
    @NotBlank(message = "请输入你的Minio的私有存储桶桶名", groups = MinioGroup.class)
    private String minioPrivateBucketName;
    @Schema(description = "Minio的用户信息存储桶桶名")
    @NotBlank(message = "请输入你的Minio的用户信息存储桶桶名", groups = MinioGroup.class)
    private String minioUserInfoBucketName;

    public Class<?> getGroup(StorageTypeEnum type) {
        Map<StorageTypeEnum, Class<?>> classMap = Map.of(
                StorageTypeEnum.OSS, OssGroup.class,
                StorageTypeEnum.COS, CosGroup.class,
                StorageTypeEnum.KODO, KodoGroup.class,
                StorageTypeEnum.MINIO, MinioGroup.class,
                StorageTypeEnum.LOCAL, LocalGroup.class
        );
        Class<?> storageType = classMap.get(type);

        if (ObjectUtils.isEmpty(storageType)) {
            log.warn("不支持的云服务类型");
            throw new IllegalArgumentException("不支持的云服务类型");
        }

        return storageType;
    }

    public interface OssGroup {
    }

    public interface CosGroup {
    }

    public interface KodoGroup {
    }

    public interface MinioGroup {
    }

    public interface LocalGroup {
    }
}