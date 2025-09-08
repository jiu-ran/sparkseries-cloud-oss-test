package com.sparkseries.module.oss.file.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件夹信息VO
 */
@Data
@NoArgsConstructor
public class FolderInfoVO {

    private String folderPath;
    private String folderName;

    public FolderInfoVO(String folderName, String folderPath) {
        this.folderName = folderName;
        this.folderPath = folderPath;
    }

}