package com.sparkseries.module.oss.common.api.provider.service;


import com.sparkeries.dto.UploadFileDTO;
import com.sparkeries.enums.StorageTypeEnum;
import com.sparkeries.enums.VisibilityEnum;
import com.sparkseries.module.oss.file.vo.FilesAndFoldersVO;

/**
 * 文件存储服务接口
 */
public interface OssService {

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 操作结果
     */
    boolean uploadFile(UploadFileDTO file);

    /**
     * 创建文件夹
     *
     * @param folderName 文件夹名称
     * @param folderPath 文件夹路径
     * @param visibility 能见度
     * @param userId 用户 id
     * @return 操作结果
     */
    boolean createFolder(String folderName, String folderPath, VisibilityEnum visibility, String userId);

    /**
     * 删除文件
     *
     * @param fileName 文件名
     * @param folderPath 文件夹路径
     * @param visibility 能见度
     * @param userId 用户 id
     * @return 操作结果
     */
    boolean deleteFile(String fileName, String folderPath, VisibilityEnum visibility, String userId);

    /**
     * 删除文件夹
     *
     * @param folderName 文件夹名称
     * @param folderPath 文件夹路径
     * @param visibility 能见度
     * @param userId 用户 id
     * @return 操作结果
     */
    boolean deleteFolder(String folderName, String folderPath, VisibilityEnum visibility, String userId);

    /**
     * 下载文件
     *
     * @param fileName 文件名
     * @param folderPath 文件夹路径
     * @param visibility 能见度
     * @param userId 用户 id
     * @return 文件下载 url
     */
    String downLoad(String fileName, String folderPath, VisibilityEnum visibility, String userId);

    /**
     * 列出文件夹下的文件及文件夹
     *
     * @param folderName 文件夹名称
     * @param folderPath 文件夹路径
     * @param visibility 能见度
     * @param userId 用户 id
     * @return 文件夹下的文件和文件夹列表
     */
    FilesAndFoldersVO listFileAndFolder(String folderName, String folderPath, VisibilityEnum visibility, Long userId);

    /**
     * 获取文件预览 url
     *
     * @param fileName 文件名
     * @param folderPath 文件夹路径
     * @param visibility 能见度
     * @param userId 用户 id
     * @return 文件预览 url
     */
    String previewFile(String fileName, String folderPath, VisibilityEnum visibility, String userId);

    /**
     * 移动文件到另一个文件夹
     *
     * @param fileName 文件名
     * @param sourceFolderPath 源文件夹路径
     * @param targetFolderPath 目标文件夹路径
     * @param visibility 能见度
     * @param userId 用户 id
     * @return 操作结果
     */
    boolean moveFile(String fileName, String sourceFolderPath, String targetFolderPath, VisibilityEnum visibility, String userId);


    /**
     * 获取此服务的存储类型
     *
     * @return 存储类型枚举
     */
    StorageTypeEnum getStorageType();
}
