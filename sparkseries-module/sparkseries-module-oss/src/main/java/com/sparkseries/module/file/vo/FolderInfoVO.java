package com.sparkseries.module.file.vo;

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

    public FolderInfoVO(String folderPath) {
        this.folderPath = folderPath;
        String[] filename = folderPath.split("/");
        this.folderName = filename[filename.length - 1];

    }

}