package com.sparkseries.module.oss.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件夹信息VO
 */
@Data
@NoArgsConstructor
@Schema(description = "文件夹信息")
public class FolderInfoVO {
    @Schema(description = "文件夹路径")
    private String folderPath;
    @Schema(description = "文件夹名")
    private String folderName;

    public FolderInfoVO(String folderName, String folderPath) {
        this.folderName = folderName;
        this.folderPath = folderPath;
    }

}