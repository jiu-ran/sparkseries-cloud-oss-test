package com.sparkseries.module.file.entity;

import com.sparkseries.common.enums.StorageTypeEnum;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FolderMetadataEntity {

    private Long id;

    private String folderName;

    private String absolutePath;

    private LocalDateTime lastUpdateDate;

    private StorageTypeEnum storageType;

}
