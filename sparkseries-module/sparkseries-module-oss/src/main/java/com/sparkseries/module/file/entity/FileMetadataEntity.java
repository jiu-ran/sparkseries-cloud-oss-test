package com.sparkseries.module.file.entity;

import com.sparkseries.common.enums.StorageTypeEnum;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件元数据信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileMetadataEntity {

    private Long id;
    private String filename;
    private String originalName;
    private String fileType;
    private String fileSize;
    private String storagePath;
    private LocalDateTime lastUpdateDate;
    private StorageTypeEnum storageType;


}