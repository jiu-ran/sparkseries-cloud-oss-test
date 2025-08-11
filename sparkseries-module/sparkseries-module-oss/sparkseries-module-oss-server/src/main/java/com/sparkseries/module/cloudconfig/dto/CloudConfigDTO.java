package com.sparkseries.module.cloudconfig.dto;


import com.sparkeries.enums.StorageTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 云存储配置数据传输对象
 */
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
    @Schema(description = "阿里云OSS的bucketName")
    @NotBlank(message = "请输入你的阿里云OSS的bucketName", groups = OssGroup.class)
    private String ossBucketName;
    @Schema(description = "阿里云OSS所属地区")
    @NotBlank(message = "请输入你的阿里云OSS所属地", groups = OssGroup.class)
    private String ossRegion;
    @Schema(description = "腾讯云COS的secretId")
    @NotBlank(message = "请输入你的腾讯云COS的secretId", groups = CosGroup.class)
    private String cosSecretId;
    @Schema(description = "腾讯云COS的secretKey")
    @NotBlank(message = "请输入你的腾讯云COS的secretKey", groups = CosGroup.class)
    private String cosSecretKey;
    @Schema(description = "腾讯云COS的bucketName")
    @NotBlank(message = "请输入你的腾讯云COS的bucketName", groups = CosGroup.class)
    private String cosBucketName;
    @Schema(description = "腾讯云COS的所属地区")
    @NotBlank(message = "请输入你的腾讯云COS的所属地", groups = CosGroup.class)
    private String cosRegion;
    @Schema(description = "七牛KODO的accessKey")
    @NotBlank(message = "请输入你的七牛云KODO的accessKey", groups = KodoGroup.class)
    private String kodoAccessKey;
    @Schema(description = "七牛KODO的secretKey")
    @NotBlank(message = "请输入你的七牛云KODO的secretKey", groups = KodoGroup.class)
    private String kodoSecretKey;
    @Schema(description = "七牛存KODO的bucketName")
    @NotBlank(message = "请输入你的七牛云KODO的bucketName", groups = KodoGroup.class)
    private String kodoBucketName;
    @Schema(description = "Minio的endPoint")
    @NotBlank(message = "请输入你的Minio的endpoint", groups = MinioGroup.class)
    private String minioEndPoint;
    @Schema(description = "Minio的accessKey")
    @NotBlank(message = "请输入你的Minio的accessKey", groups = MinioGroup.class)
    private String minioAccessKey;
    @Schema(description = "Minio的secretKey")
    @NotBlank(message = "请输入你的Minio的secretKey", groups = MinioGroup.class)
    private String minioSecretKey;
    @Schema(description = "Minio的BucketName")
    @NotBlank(message = "请输入你的Minio的bucketName", groups = MinioGroup.class)
    private String minioBucketName;

    public Class<?> getGroup(StorageTypeEnum type) {
        Map<StorageTypeEnum, Class<?>> classMap = Map.of(
                StorageTypeEnum.OSS, OssGroup.class,
                StorageTypeEnum.COS, CosGroup.class,
                StorageTypeEnum.KODO, KodoGroup.class,
                StorageTypeEnum.MINIO, MinioGroup.class,
                StorageTypeEnum.LOCAL, LocalGroup.class
        );
        return classMap.get(type);
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