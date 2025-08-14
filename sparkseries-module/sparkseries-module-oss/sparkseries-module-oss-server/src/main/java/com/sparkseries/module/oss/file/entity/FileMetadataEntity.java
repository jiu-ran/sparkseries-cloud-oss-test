package com.sparkseries.module.oss.file.entity;

import com.sparkeries.enums.StorageTypeEnum;
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

    private String fileName;

    private String originalName;

    private String fileType;

    private String fileSize;

    private String storagePath;

    private LocalDateTime lastUpdateDate;

    private StorageTypeEnum storageType;


}