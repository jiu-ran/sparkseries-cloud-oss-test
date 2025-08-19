package com.sparkseries.module.oss.file.entity;

import com.sparkeries.enums.StorageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FolderMetadataEntity {

    private Long id;

    private Long userId;

    private String folderName;

    private String storagePath;

    private String absolutePath;

    private LocalDateTime lastUpdateDate;

    private StorageTypeEnum storageType;

}
