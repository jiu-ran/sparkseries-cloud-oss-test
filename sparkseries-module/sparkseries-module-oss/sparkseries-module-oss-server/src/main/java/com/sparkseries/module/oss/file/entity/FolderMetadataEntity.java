package com.sparkseries.module.oss.file.entity;

import com.sparkeries.enums.StorageTypeEnum;
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
