package com.sparkseries.module.oss.file.entity;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkeries.enums.VisibilityEnum;
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
public class FileMetadataEntity {

    private Long id;

    private Long userId;

    private String fileName;

    private String fileType;

    private String fileSize;

    private String folderPath;

    private LocalDateTime lastUpdateDate;

    private StorageTypeEnum storageType;

    private VisibilityEnum visibility;


}