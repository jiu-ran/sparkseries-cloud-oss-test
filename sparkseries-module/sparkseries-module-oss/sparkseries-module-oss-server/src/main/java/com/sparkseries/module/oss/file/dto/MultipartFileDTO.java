package com.sparkseries.module.oss.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.InputStream;

/**
 * 文件信息
 */
@Data
@Builder
@Schema(description = "文件信息")
public class MultipartFileDTO {
    @Schema(description = "文件 id")
    private Long id;
    @Schema(description = "用户 id")
    private Long userId;
    @Schema(description = "文件名")
    private String fileName;
    @Schema(description = "文件输入流")
    private InputStream inputStream;
    @Schema(description = "文件大小")
    private Long size;
    @Schema(description = "文件类型")
    private String type;
}