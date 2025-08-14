package com.sparkseries.module.oss.file.vo;


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


}