package com.sparkeries.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * 文件存储DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "文件存储DTO")
public class FileStorageDTO {
    @Schema(description = "用户id")
    private String userId;
    @Schema(description = "文件数据流")
    private InputStream inputStream;
    @Schema(description = "文件名")
    private String filename;
    @Schema(description = "文件大小")
    private Long size;
    @Schema(description = "绝对路径")
    private String absolutePath;
    @Schema(description = "父路径路径")
    private Path path;

    public FileStorageDTO(String userId, InputStream inputStream, String filename, Long size, String absolutePath) {
        this.userId = userId;
        this.inputStream = inputStream;
        this.filename = filename;
        this.size = size;
        this.absolutePath = absolutePath;
    }
}
