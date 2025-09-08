package com.sparkseries.module.oss.file.dto;

import lombok.Builder;
import lombok.Data;

import java.io.InputStream;

/**
 * 多部分文件数据传输对象
 * 用于封装文件上传时的相关信息
 */
@Data
@Builder
public class MultipartFileDTO {

    private Long id;

    private Long userId;

    private String fileName;

    private InputStream inputStream;

    private Long size;

    private String strSize;

    private String type;
}