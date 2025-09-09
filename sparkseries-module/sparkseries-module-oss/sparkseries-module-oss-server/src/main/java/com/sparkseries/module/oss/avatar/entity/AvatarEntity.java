package com.sparkseries.module.oss.avatar.entity;

import com.sparkeries.enums.StorageTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 用户头像
 */
@Data
@Builder
@Schema(description = "用户头像")
public class AvatarEntity {
    @Schema(description = "用户 ID")
    private Long userId;
    @Schema(description = "头像扩展名")
    private String suffixName;
    @Schema(description = "头像大小")
    private String size;
    @Schema(description = "头像存储的文件夹路径")
    private String folderPath;
    @Schema(description = "头像的存储方式(OSS COS KODO Minio Local)")
    private StorageTypeEnum storageType;
}
