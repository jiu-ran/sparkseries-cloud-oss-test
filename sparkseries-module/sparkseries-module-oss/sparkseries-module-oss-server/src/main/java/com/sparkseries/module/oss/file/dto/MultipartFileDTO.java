package com.sparkseries.module.oss.file.dto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;

/**
 * 多部分文件数据传输对象
 * 用于封装文件上传时的相关信息
 */
@Data
@Slf4j
public class MultipartFileDTO {

    private String id;

    private Long userId;

    private String filename;

    private InputStream inputStream;

    private Long size;

    private String strSize;

    private String type;

    private String absolutePath;

    /**
     * 构造方法
     *
     * @param filename 文件名
     * @param inputStream 文件输入流
     * @param size 文件大小
     * @param type 文件类型
     */
    public MultipartFileDTO(Long userId, String filename, InputStream inputStream, Long size, String type) {
        this.userId = userId;
        this.filename = filename;
        this.inputStream = inputStream;
        this.size = size;
        this.type = type;
    }

    public MultipartFileDTO(String filename, InputStream inputStream, Long size, String type) {
        this.filename = filename;
        this.inputStream = inputStream;
        this.size = size;
        this.type = type;
    }


}
