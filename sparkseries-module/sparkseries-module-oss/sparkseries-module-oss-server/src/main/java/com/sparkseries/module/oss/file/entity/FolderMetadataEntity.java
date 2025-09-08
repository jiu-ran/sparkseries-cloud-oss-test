package com.sparkseries.module.oss.file.entity;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkeries.enums.VisibilityEnum;
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

    private String folderPath;

    private LocalDateTime lastUpdateDate;

    private StorageTypeEnum storageType;

    private VisibilityEnum visibility;

}
