package com.sparkseries.module.oss.file.service;

import com.sparkeries.enums.VisibilityEnum;
import com.sparkseries.common.util.entity.Result;
import com.sparkseries.module.oss.file.dto.MultipartFileDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 文件服务接口，定义了文件管理相关的操作
 */
public interface FileService {

    /**
     * 文件列表上传
     *
     * @param files 待上传的文件列表
     * @param path 文件存储的文件夹路径
     * @param visibility 能见度
     * @return 上传结果
     */
    Result<?> uploadFiles(List<MultipartFileDTO> files, String path, VisibilityEnum visibility);

    /**
     * 创建文件夹
     *
     * @param path 文件夹路径
     * @param folderName 文件夹名
     * @param visibility 能见度
     * @return 创建文件夹结果
     */
    Result<?> createFolder(String path, String folderName, VisibilityEnum visibility);

    /**
     * 删除文件
     *
     * @param id 文件 ID
     * @param visibility 文件能见度
     * @return 删除结果
     */
    Result<?> deleteFile(Long id, VisibilityEnum visibility);

    /**
     * 删除文件夹及文件夹下的文件
     *
     * @param path 文件夹路径
     * @param visibility 能见度
     * @return 删除结果
     */
    Result<?> deleteFolder(String path, VisibilityEnum visibility);

    /**
     * 文件重命名
     *
     * @param id 文件 ID
     * @param name 新名称
     * @param visibility 能见度
     * @return 重命名结果
     */
    Result<?> rename(Long id, String name, VisibilityEnum visibility);

    /**
     * 移动文件
     *
     * @param id 文件 ID
     * @param path 目标文件夹路径
     * @param visibility 能见度
     * @return 移动结果
     */
    Result<?> moveFile(Long id, String path, VisibilityEnum visibility);

    /**
     * 列出指定路径下的文件和文件夹
     *
     * @param path 文件夹路径
     * @param visibility 能见度
     * @return 文件和文件夹列表
     */
    Result<?> listFiles(String path, VisibilityEnum visibility);

    /**
     * 获取文件的预览 url
     *
     * @param id 待预览文件的ID
     * @param visibility 能见度
     * @return 预览 url
     */
    Result<?> previewUrl(Long id, VisibilityEnum visibility);

    /**
     * 获取文件的下载 url
     *
     * @param id 文件 ID
     * @param visibility 能见度
     * @return 下载 url
     */
    Result<?> downloadFile(Long id, VisibilityEnum visibility);

    /**
     * 预览本地文件
     *
     * @param id 文件 ID
     * @param visibility 能见度
     * @return 预览文件响应
     */
    ResponseEntity<?> previewLocalFile(Long id, VisibilityEnum visibility);

    /**
     * 下载本地文件
     *
     * @param id 文件 ID
     * @param visibility 能见度
     * @return 文件下载响应
     */
    ResponseEntity<?> downloadLocalFile(Long id, VisibilityEnum visibility);

}
