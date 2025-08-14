package com.sparkseries.module.oss.file.service;

import com.sparkseries.module.oss.file.dto.MultipartFileDTO;
import com.sparkseries.common.util.entity.Result;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 文件服务接口，定义了文件管理相关的操作
 */
public interface FileService {

    /**
     * 上传文件到云存储
     *
     * @param files 待上传的文件列表
     * @param path  上传的目标路径
     * @return 包含操作结果的Result对象
     */
    Result<?> uploadFiles(List<MultipartFileDTO> files, String path);

    /**
     * 在云存储中创建文件夹
     *
     * @param path 待创建文件夹的路径
     * @return 包含操作结果的Result对象
     */
    Result<?> createFolder(String path);

    /**
     * 删除云存储中的文件
     *
     * @param id 待删除文件的ID
     * @return 包含操作结果的Result对象
     */
    Result<?> deleteFile(Long id);

    /**
     * 删除云存储中的文件夹
     *
     * @param path 待删除文件夹的路径
     * @return 包含操作结果的Result对象
     */
    Result<?> deleteFolder(String path);

    /**
     * 重命名云存储中的文件或文件夹
     *
     * @param id   待重命名文件或文件夹的ID
     * @param name 新名称
     * @return 包含操作结果的Result对象
     */
    Result<?> rename(Long id, String name);

    /**
     * 从云存储下载文件
     *
     * @param id 待下载文件的ID
     * @return 包含操作结果的Result对象
     */
    Result<?> downloadFile(Long id);

    /**
     * 下载本地文件
     *
     * @param id 待下载本地文件的ID
     * @return 包含文件内容的ResponseEntity对象
     */
    ResponseEntity<?> downloadLocalFile(Long id);

    /**
     * 列出指定路径下的文件和文件夹
     *
     * @param path 待列出内容的路径
     * @return 包含文件和文件夹列表的Result对象
     */
    Result<?> listFiles(String path);

    /**
     * 获取云存储中文件的预览URL
     *
     * @param id 待预览文件的ID
     * @return 包含预览URL的Result对象
     */
    Result<?> previewUrl(Long id);

    /**
     * 预览本地文件
     *
     * @param id 待预览本地文件的ID
     * @return 包含文件内容的ResponseEntity对象
     */
    ResponseEntity<?> previewLocalFile(Long id);

    /**
     * 移动云存储中的文件
     *
     * @param id   待移动文件的ID
     * @param path 移动的目标路径
     * @return 包含操作结果的Result对象
     */
    Result<?> moveFile(Long id, String path);


}
