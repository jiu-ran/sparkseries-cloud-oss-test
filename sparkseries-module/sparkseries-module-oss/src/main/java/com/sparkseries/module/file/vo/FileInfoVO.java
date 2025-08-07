package com.sparkseries.module.file.vo;


import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件信息VO
 */
@Data
@NoArgsConstructor
public class FileInfoVO {

    private String id;
    private String originalName;
    private String fileSize;
    private String lastUpdateDate;


    public FileInfoVO(String id, String originalName, String fileSize, String lastUpdateDate) {
        this.id = id;
        this.originalName = originalName;
        this.fileSize = fileSize;
        this.lastUpdateDate = lastUpdateDate;
    }


}