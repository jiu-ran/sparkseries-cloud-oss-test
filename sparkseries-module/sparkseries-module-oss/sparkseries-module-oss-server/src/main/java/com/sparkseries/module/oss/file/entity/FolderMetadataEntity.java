package com.sparkseries.module.oss.file.entity;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkeries.enums.VisibilityEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "文件夹元数据")
public class FolderMetadataEntity {
    @Schema(description = "文件夹 id")
    private Long id;
    @Schema(description = "用户 id")
    private Long userId;
    @Schema(description = "文件夹名")
    private String folderName;
    @Schema(description = "文件夹路径")
    private String folderPath;
    @Schema(description = "最后更新时间")
    private LocalDateTime lastUpdateDate;
    @Schema(description = "文件夹存储类型")
    private StorageTypeEnum storageType;
    @Schema(description = "文件夹可见性")
    private VisibilityEnum visibility;

}
