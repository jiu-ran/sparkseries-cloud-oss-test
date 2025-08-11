package com.sparkeries.dto;

import lombok.Data;

@Data
public class AvatarDTO {
    public Long id;
    private String size;
    private String absolutePath;
    private Long userId;
    private String storageType;

    public AvatarDTO(Long id, Long userId, String absolutePath, String size, String storageType) {
        this.id = id;
        this.userId = userId;
        this.absolutePath = absolutePath;
        this.size = size;
        this.storageType = storageType;
    }

}