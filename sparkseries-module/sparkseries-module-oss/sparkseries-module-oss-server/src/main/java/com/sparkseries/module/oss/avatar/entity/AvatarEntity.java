package com.sparkseries.module.oss.avatar.entity;

import com.sparkeries.enums.StorageTypeEnum;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AvatarEntity {
    private Long userId;
    private String size;
    private String folderPath;
    private StorageTypeEnum storageType;
    private String suffixName;
}
