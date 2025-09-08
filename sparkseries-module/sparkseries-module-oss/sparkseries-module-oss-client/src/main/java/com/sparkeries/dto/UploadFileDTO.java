package com.sparkeries.dto;

import com.sparkeries.enums.VisibilityEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.InputStream;

/**
 * 文件存储DTO
 */
@Data
@Builder
@Schema(description = "文件信息")
public class UploadFileDTO {
    @Schema(description = "用户id")
    private String userId;
    @Schema(description = "文件数据流")
    private InputStream inputStream;
    @Schema(description = "文件名")
    private String fileName;
    @Schema(description = "文件大小")
    private Long size;
    @Schema(description = "文件存储文件夹路径")
    private String folderPath;
    @Schema(description = "文件可见性")
    private VisibilityEnum visibility;
    @Schema(description = "文件在OSS中的存储位置 绝对路径")
    private String targetPath;
}
