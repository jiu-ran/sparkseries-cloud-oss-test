package com.sparkseries.module.oss.file.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件信息
 */
@Data
@NoArgsConstructor
@Schema(description = "文件信息")
public class FileInfoVO {

    @Schema(description = "文件 id")
    private String id;
    @Schema(description = "文件名")
    private String fileName;
    @Schema(description = "文件类型")
    private String fileSize;
    @Schema(description = "文件大小")
    private String lastUpdateDate;


}