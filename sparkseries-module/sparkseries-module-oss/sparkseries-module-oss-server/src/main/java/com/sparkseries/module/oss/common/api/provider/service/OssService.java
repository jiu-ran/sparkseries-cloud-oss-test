package com.sparkseries.module.oss.common.api.provider.service;


import com.sparkeries.dto.FileStorageDTO;
import com.sparkeries.enums.StorageTypeEnum;
import com.sparkeries.enums.VisibilityEnum;
import com.sparkseries.module.oss.file.entity.FileMetadataEntity;
import com.sparkseries.module.oss.file.vo.FilesAndFoldersVO;
import org.springframework.http.ResponseEntity;

/**
 * 文件存储服务接口
 */
public interface OssService {

    /**
     * 上传文件
     *
     * @param file 文件
     * @param visibility 能见度
     * @return 操作成功返回 true，失败返回 false
     */
    boolean uploadFile(FileStorageDTO file, VisibilityEnum visibility);


    /**
     * 上传头像
     *
     * @param avatar 头像文件
     * @return 操作成功返回 true，失败返回 false
     */
    boolean uploadAvatar(FileStorageDTO avatar);

    /**
     * 创建文件夹
     *
     * @param path 文件夹路径
     * @param visibility 能见度
     * @param userId 用户 id
     * @return 操作成功返回 true，失败返回 false
     */
    boolean createFolder(String path, VisibilityEnum visibility, String userId);

    /**
     * 删除文件
     *
     * @param absolutePath 文件的绝对路径
     * @param visibility 能见度
     * @param userId
     * @return 操作成功返回 true，失败返回 false
     */
    boolean deleteFile(String absolutePath, VisibilityEnum visibility, String userId);

    /**
     * 删除文件夹
     *
     * @param path 文件夹路径
     * @param visibility 能见度
     * @param userId
     * @return 操作成功返回 true，失败返回 false
     */
    boolean deleteFolder(String path, VisibilityEnum visibility, String userId);

    /**
     * 下载文件
     *
     * @param absolutePath 文件的绝对路径
     * @param downloadFileName 下载的文件名
     * @param visibility 能见度
     * @return 下载文件的url路径
     */
    String downLoad(String absolutePath, String downloadFileName, VisibilityEnum visibility);

    /**
     * 本地文件下载
     *
     * @param metadata 文件元数据
     * @param visibilityEnum 能见度
     * @param userId 用户 id
     * @return ResponseEntity
     */
    ResponseEntity<?> downLocalFile(FileMetadataEntity metadata, VisibilityEnum visibilityEnum, String userId);

    /**
     * 重命名文件
     *
     * @param id 文件id
     * @param filename 原始文件名
     * @param newFilename 新的文件名
     * @param path 文件夹路径
     * @param visibility 能见度
     * @param userId 用户 id
     * @return 操作成功返回 true，失败返回 false
     */
    boolean rename(Long id, String filename, String newFilename, String path,VisibilityEnum visibility, String userId);

    /**
     * 列出文件夹下的文件及文件夹
     *
     * @param path 文件夹路径
     * @return 文件夹下的文件和文件夹列表
     */
    FilesAndFoldersVO listFiles(String path);

    /**
     * 获取文件预览url
     *
     * @param absolutePath 文件的绝对路径
     * @return 文件预览url
     */
    String previewFile(String absolutePath);

    /**
     * 获取本地文件的预览url
     *
     * @param metadata 文件元数据
     * @param visibility 能见度
     * @param userId 用户 id
     * @return 文件预览url
     */
    ResponseEntity<?> previewLocalFile(FileMetadataEntity metadata, VisibilityEnum visibility, String userId);

    /**
     * 获取本地用户头像的预览url
     *
     * @param absolutePath 用户id
     * @return 文件预览url
     */
    ResponseEntity<?> previewLocalAvatar(String absolutePath);

    /**
     * 移动文件从一个路径到另一个路径。
     *
     * @param sourceAbsolutePath 原始文件的绝对路径
     * @param targetAbsolutePath 文件的目标移动路径
     * @return 操作成功返回 true，失败返回 false
     */
    boolean moveFile(String sourceAbsolutePath, String targetAbsolutePath);


    /**
     * 获取此服务的存储类型
     *
     * @return 存储类型枚举
     */
    StorageTypeEnum getStorageType();
}
