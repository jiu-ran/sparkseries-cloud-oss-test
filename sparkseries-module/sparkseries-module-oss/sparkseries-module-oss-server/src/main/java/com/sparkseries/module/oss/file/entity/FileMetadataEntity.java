package com.sparkseries.module.oss.file.entity;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkeries.enums.VisibilityEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件元数据信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "文件元数据")
public class FileMetadataEntity {
    @Schema(description = "文件 id")
    private Long id;
    @Schema(description = "用户 id")
    private Long userId;
    @Schema(description = "文件名")
    private String fileName;
    @Schema(description = "文件类型")
    private String fileType;
    @Schema(description = "文件大小")
    private String fileSize;
    @Schema(description = "文件存储文件夹")
    private String folderPath;
    @Schema(description = "文件最后更新时间")
    private LocalDateTime lastUpdateDate;
    @Schema(description = "文件存储类型")
    private StorageTypeEnum storageType;
    @Schema(description = "文件可见性")
    private VisibilityEnum visibility;


}